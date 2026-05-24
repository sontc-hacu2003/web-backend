package backend.web.auth.controller;

import backend.web.core.response.base.PublicKeyResponse;
import backend.web.core.service.MiddlewareCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "backend.cors.allowed-origins")
@RequestMapping("/auth")
@RequiredArgsConstructor
public class MiddlewareKeyController {
    private final MiddlewareCryptoService cryptoService;

    @GetMapping("/public-key")
    public PublicKeyResponse getPublicKey() {
        return new PublicKeyResponse(cryptoService.getPublicKey());
    }
}
