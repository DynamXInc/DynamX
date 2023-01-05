package fr.dynamx.api.network.sync;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SynchronizedEntityVariable
{
    String name();

    @Retention(RetentionPolicy.RUNTIME)
    @interface SynchronizedPhysicsModule {}
}
