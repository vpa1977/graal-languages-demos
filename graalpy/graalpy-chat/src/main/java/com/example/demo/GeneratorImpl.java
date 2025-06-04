package com.example.demo;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.python.embedding.GraalPyResources;
import org.graalvm.python.embedding.VirtualFileSystem;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class GeneratorImpl implements IGenerator {

    @FunctionalInterface
    interface Generate {
        String process_prompt(String prompt);
    }

    private final Context context;
    private final Generate generator;

    private void warn(Object... foo) {
        log(foo);
    }
    private void log(Object... foo) {
        for (var f : foo) {
            System.out.print(f);
        }
        System.out.println();
    }

    private static final String J_PYENVCFG = "pyvenv.cfg";

    private void findAndApplyVenvCfg(Context.Builder contextBuilder, String executable) {
        Path executablePath;
        try {
            executablePath = Paths.get(executable);
        } catch (InvalidPathException e) {
            log("cannot determine path of the executable");
            return;
        }
        Path binDir = executablePath.getParent();
        if (binDir == null) {
            log("parent directory of the executable does not exist");
            return;
        }
        Path venvCfg = binDir.resolve(J_PYENVCFG);
        log("checking: ", venvCfg);
        if (!Files.exists(venvCfg)) {
            Path binParent = binDir.getParent();
            if (binParent == null) {
                return;
            }
            venvCfg = binParent.resolve(J_PYENVCFG);
            log("checking: ", venvCfg);
            if (!Files.exists(venvCfg)) {
                return;
            }
        }
        log("found: ", venvCfg);
        try (BufferedReader reader = Files.newBufferedReader(venvCfg)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length != 2) {
                    continue;
                }
                String name = parts[0].trim();
                switch (name) {
                    case "home":
                        try {
                            Path homeProperty = Paths.get(parts[1].trim());
                            Path graalpyHome = homeProperty;
                            /*
                             * (tfel): According to PEP 405, the home key is the directory of the
                             * Python executable from which this virtual environment was created,
                             * that is, it usually ends with "/bin" on a Unix system. On Windows,
                             * the base Python should be in the top-level directory or under
                             * "\Scripts". To support running from Maven artifacts where we don't
                             * have a working executable, we patched our shipped venv module to set
                             * the home path without a "/bin" or "\\Scripts" suffix, so we
                             * explicitly check for those two subfolder cases and otherwise assume
                             * the home key is directly pointing to the Python home.
                             */
                            if (graalpyHome.endsWith("bin") || graalpyHome.endsWith("Scripts")) {
                                graalpyHome = graalpyHome.getParent();
                            }
                            contextBuilder.option("python.PythonHome", graalpyHome.toString());
                            /*
                             * First try to resolve symlinked executables, since that may be more
                             * accurate than assuming the executable in 'home'.
                             */
                            Path baseExecutable = null;
                            try {
                                Path realPath = executablePath.toRealPath();
                                if (!realPath.equals(executablePath.toAbsolutePath())) {
                                    baseExecutable = realPath;
                                }
                            } catch (IOException ex) {
                                // Ignore
                            }
                            if (baseExecutable == null) {
                                baseExecutable = homeProperty.resolve(executablePath.getFileName());
                            }
                            if (Files.exists(baseExecutable)) {
                                contextBuilder.option("python.BaseExecutable", baseExecutable.toString());
                                /*
                                 * This is needed to support the legacy GraalVM layout where the
                                 * executable is a symlink into the 'languages' directory.
                                 */
                                contextBuilder.option("python.PythonHome", baseExecutable.getParent().getParent().toString());
                            }
                        } catch (NullPointerException | InvalidPathException ex) {
                            // NullPointerException covers the possible null result of getParent()
                            warn("Could not set PYTHONHOME according to the pyvenv.cfg file.");
                        }
                        break;
                    case "venvlauncher_command":
                        contextBuilder.option("python.VenvlauncherCommand", parts[1].trim());
                        break;
                    case "base-prefix":
                        contextBuilder.option("python.SysBasePrefix", parts[1].trim());
                        break;
                    case "base-executable":
                        contextBuilder.option("python.BaseExecutable", parts[1].trim());
                        break;
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not read the pyvenv.cfg file.");
        }
    }


    public GeneratorImpl() {
        Context.Builder builder = GraalPyResources.contextBuilder();

        builder = builder
                        .allowExperimentalOptions(true)
                        .option("python.PosixModuleBackend", "native")
                .option("python.Executable", "/home/bsp/demo/graalpy-demo/env/bin/python")
                .option("log.level", "INFO")
                .option("dap", "localhost:4711")
                .option("dap.Suspend", "false")
                .allowIO(IOAccess.ALL)
                .allowHostAccess(HostAccess.ALL)
                .allowNativeAccess(true)
                .allowAllAccess(true)
                .allowCreateThread(true);
        context = builder.build();
        
        context.eval("python",
"""
from transformers import AutoTokenizer, BitsAndBytesConfig, Gemma3ForCausalLM
import torch

model_id = "google/gemma-3-1b-it"
quantization_config = BitsAndBytesConfig(load_in_8bit=True)
model = Gemma3ForCausalLM.from_pretrained(
    model_id, quantization_config=quantization_config
).eval()
tokenizer = AutoTokenizer.from_pretrained(model_id)

def process_prompt(prompt):
    messages = [
        [
            {
                "role": "system",
                "content": [{"type": "text", "text": "You are a helpful assistant."},]
            },
            {
                "role": "user",
                "content": [{"type": "text", "text": prompt },]
            },
        ],
    ]
    inputs = tokenizer.apply_chat_template(
        messages,
        add_generation_prompt=True,
        tokenize=True,
        return_dict=True,
        return_tensors="pt",
    ).to(model.device)

    with torch.inference_mode():
        outputs = model.generate(**inputs, max_new_tokens=64)

    return "<br>".join([str(x) for x in tokenizer.batch_decode(outputs)])
"""
);
        generator = context.getBindings("python").getMember("process_prompt").as(Generate.class);

    }

    @PreDestroy
    public void close() {
        context.close();
    }

    @Override
    public synchronized String generate(String text) {
            String response =  generator.process_prompt(text);
            int idx = response.indexOf("<start_of_turn>model");
            if (idx > 0) {
                response = response.substring(idx + "<start_of_turn>model".length());
            }
            return response;
    }
}
