package com.sprint.client.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class InternalApiKeyFilterTest {

    private static final String TEST_KEY = "my-secret-test-key";
    private InternalApiKeyFilter filter;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new InternalApiKeyFilter(TEST_KEY);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("should skip filtering for public /api/** requests")
    void shouldSkipNonInternalRequests() {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/clients");

        assertTrue(filter.shouldNotFilter(request));

    }

    @Test
    @DisplayName("should return 401 when header is missing")
    void shouldReturn401WhenHeaderMissing() throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/internal/v1/clients/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

    }

    @Test
    @DisplayName("should return 401 when header is invalid")
    void shouldReturn401WhenHeaderInvalid() throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/internal/v1/clients/1");
        request.addHeader(InternalApiKeyFilter.INTERNAL_API_KEY_HEADER, "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(filterChain);

    }

    @Test
    @DisplayName("should set ROLE_INTERNAL_SERVICE and proceed when key is valid")
    void shouldAuthenticateWhenKeyValid() throws ServletException, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/internal/v1/clients/1");
        request.addHeader(InternalApiKeyFilter.INTERNAL_API_KEY_HEADER, TEST_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("ROLE_INTERNAL_SERVICE",
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority()
        );

    }


}