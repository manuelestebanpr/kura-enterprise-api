package co.com.kura.enterprise.api.controller;

import co.com.kura.enterprise.api.dto.AuthResponse;
import co.com.kura.enterprise.api.dto.LoginRequest;
import co.com.kura.enterprise.api.dto.OtpRequest;
import co.com.kura.enterprise.api.dto.RegisterRequest;
import co.com.kura.enterprise.config.GlobalExceptionHandler;
import co.com.kura.enterprise.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /auth/login — valid credentials returns 200 + KURA_SESSION cookie + token=null in body")
    void login_success() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt-secret-token-123")
                .userId(userId)
                .email("juan@kura.com.co")
                .fullName("Juan Pérez")
                .role("PATIENT")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        LoginRequest request = new LoginRequest();
        request.setEmail("juan@kura.com.co");
        request.setPassword("SecurePass123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("KURA_SESSION=jwt-secret-token-123")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("Secure")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(jsonPath("$.token").value(nullValue()))
                .andExpect(jsonPath("$.email").value("juan@kura.com.co"))
                .andExpect(jsonPath("$.fullName").value("Juan Pérez"));
    }

    @Test
    @DisplayName("POST /auth/login — wrong password returns 400")
    void login_wrongPassword() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid email or password"));

        LoginRequest request = new LoginRequest();
        request.setEmail("juan@kura.com.co");
        request.setPassword("WrongPass!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /auth/otp/send — valid email returns 200 + success message")
    void sendOtp_success() throws Exception {
        doNothing().when(authService).sendOtp("maria@kura.com.co");

        OtpRequest request = new OtpRequest();
        request.setEmail("maria@kura.com.co");

        mockMvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent successfully"));
    }

    @Test
    @DisplayName("POST /auth/register — without consent returns 400")
    void register_noConsent() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Debe aceptar la política de tratamiento de datos (Ley 1581)"));

        RegisterRequest request = new RegisterRequest();
        request.setCedula("1234567890");
        request.setFullName("Carlos López");
        request.setEmail("carlos@test.com");
        request.setPassword("SecurePass123!");
        request.setPhone("+573001234567");
        request.setConsentLey1581(false);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Debe aceptar la política de tratamiento de datos (Ley 1581)"));
    }
}
