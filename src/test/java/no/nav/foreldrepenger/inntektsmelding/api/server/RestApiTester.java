package no.nav.foreldrepenger.inntektsmelding.api.server;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import no.nav.foreldrepenger.inntektsmelding.api.server.app.api.ApiConfig;

public class RestApiTester {


    static Collection<Method> finnAlleRestMetoder() {
        List<Method> liste = new ArrayList<>();
        for (var klasse : finnAlleRestTjenester()) {
            for (var method : klasse.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers())) {
                    liste.add(method);
                }
            }
        }
        return liste;
    }

    static Collection<Class<?>> finnAlleRestTjenester() {
        return new ArrayList<>(finnAlleRestTjenester(new ApiConfig()));
    }

    static Collection<Class<?>> finnAlleRestTjenester(Application config) {
        return config.getClasses().stream()
            .filter(c -> c.getAnnotation(Path.class) != null)
            .toList();
    }
}
