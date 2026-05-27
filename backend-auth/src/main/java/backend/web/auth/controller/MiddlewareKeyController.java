package backend.web.auth.controller;

import backend.web.core.service.MiddlewareCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class MiddlewareKeyController {
    private final MiddlewareCryptoService cryptoService;

    @GetMapping("/public-key")
    public Map<String, String> getPublicKey() {
        return Map.of("publicKey", cryptoService.getPublicKey());
    }
}
