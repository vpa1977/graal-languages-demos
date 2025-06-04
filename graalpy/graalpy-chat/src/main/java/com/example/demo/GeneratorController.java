package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Controller
public class GeneratorController {

	@Autowired
	private IGenerator generator;

	@GetMapping("/")
	public String index() {
		return "index";
	}

	@GetMapping(value = "/generate/{text}", produces = MediaType.IMAGE_PNG_VALUE)
	@ResponseBody
	public String generate(@PathVariable("text") String encodedText) {
		String text = URLDecoder.decode(encodedText, StandardCharsets.UTF_8);
		return generator.generate(text).replace("\n", "<br>");
	}
}
