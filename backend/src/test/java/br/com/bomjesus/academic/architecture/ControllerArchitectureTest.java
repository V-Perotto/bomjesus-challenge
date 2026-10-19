package br.com.bomjesus.academic.architecture;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Princípio II: nenhum controller recebe ou devolve entidades JPA, nem dentro de tipos genéricos
 * como {@code ResponseEntity<List<...>>}.
 */
class ControllerArchitectureTest {

    private static final String CONTROLLER_PACKAGE = "br.com.bomjesus.academic.controller";

    @Test
    @DisplayName("Controllers não expõem tipos @Entity em assinaturas públicas")
    void controllersShouldNotExposeEntities() throws ClassNotFoundException {
        List<Class<?>> controllers = findControllers();
        List<String> violations = new ArrayList<>();

        for (Class<?> controller : controllers) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) {
                    continue;
                }
                List<Type> types = new ArrayList<>(Arrays.asList(method.getGenericParameterTypes()));
                types.add(method.getGenericReturnType());
                types.forEach(type -> collectEntities(type, controller.getSimpleName() + "#" + method.getName(),
                        violations));
            }
        }

        assertThat(controllers).isNotEmpty();
        assertThat(violations).as("entidades expostas em controllers").isEmpty();
    }

    private static List<Class<?>> findControllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<BeanDefinition> candidates = scanner.findCandidateComponents(CONTROLLER_PACKAGE);
        List<Class<?>> controllers = new ArrayList<>();
        for (BeanDefinition candidate : candidates) {
            controllers.add(Class.forName(candidate.getBeanClassName()));
        }
        return controllers;
    }

    private static void collectEntities(Type type, String location, List<String> violations) {
        switch (type) {
            case Class<?> clazz -> {
                if (clazz.isAnnotationPresent(Entity.class)) {
                    violations.add(location + " -> " + clazz.getSimpleName());
                }
                if (clazz.isArray()) {
                    collectEntities(clazz.getComponentType(), location, violations);
                }
            }
            case ParameterizedType parameterized -> {
                collectEntities(parameterized.getRawType(), location, violations);
                for (Type argument : parameterized.getActualTypeArguments()) {
                    collectEntities(argument, location, violations);
                }
            }
            case WildcardType wildcard -> {
                for (Type bound : wildcard.getUpperBounds()) {
                    collectEntities(bound, location, violations);
                }
            }
            case GenericArrayType array -> collectEntities(array.getGenericComponentType(), location, violations);
            default -> {
                // variáveis de tipo não carregam entidade concreta
            }
        }
    }
}
