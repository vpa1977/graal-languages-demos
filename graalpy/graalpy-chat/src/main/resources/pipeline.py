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

    return "".join([str(x) for x in tokenizer.batch_decode(outputs)])