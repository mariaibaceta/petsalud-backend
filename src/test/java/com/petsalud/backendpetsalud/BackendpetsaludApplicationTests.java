package com.petsalud.backendpetsalud;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** El contexto completo levanta con el perfil test (H2 en memoria, sin MySQL). */
@SpringBootTest
@ActiveProfiles("test")
class BackendpetsaludApplicationTests {

	@Test
	void contextLoads() {
	}

}
