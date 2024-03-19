package org.cryptomator.hub.rollback;

import io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterTestExecutionCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Nested;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class DBRollbackExtension implements QuarkusTestAfterConstructCallback, QuarkusTestAfterTestExecutionCallback {

	static final AtomicReference<Flyway> INSTANCE = new AtomicReference<>(null);

	@Override
	public void afterTestExecution(QuarkusTestMethodContext context) {
		var isAnnotationPresent = context.getTestMethod().getAnnotation(DBRollback.class) != null;
		if(isAnnotationPresent) {
			var flyway = INSTANCE.get();
			if(flyway == null) {
				throw new IllegalStateException("Flyway instance was not set. Please ensure that test class (or enclosing class) have a public non-null, Flyway field.");
			}

			flyway.clean();
			flyway.migrate();
		}
	}

	@Override
	public void afterConstruct(Object testInstance) {
		try {
			var topLevelTestInstance = getTopLevelTestInstance(testInstance);
			INSTANCE.set(getFlywayObject(topLevelTestInstance));
		} catch (NoSuchFieldException | IllegalAccessException e) {
			//no-op
		}
	}

	private Object getTopLevelTestInstance(Object testInstance) throws NoSuchFieldException, IllegalAccessException {
		var testClazz = testInstance.getClass();
		var hasEnclosingClass = testClazz.getEnclosingClass() != null;
		var isNotStatic = !Modifier.isStatic(testClazz.getModifiers());
		var isNested = testClazz.getAnnotation(Nested.class) != null;
		if(hasEnclosingClass && isNotStatic && isNested) {
			Field field = testClazz.getDeclaredField("this$0");
			field.setAccessible(true);
			return getTopLevelTestInstance(field.get(testInstance));
		} else {
			return testInstance;
		}
	}

	private Flyway getFlywayObject(Object obj) throws NoSuchFieldException, IllegalAccessException {
		var flywayField = Arrays.stream(obj.getClass().getFields()).filter(field -> field.getType().equals(Flyway.class)).findFirst().orElseThrow(NoSuchFieldException::new);
		return (Flyway) flywayField.get(obj);
	}
}
