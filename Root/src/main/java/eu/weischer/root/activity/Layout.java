package eu.weischer.root.activity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Layout {
    public Class bindingClass() default void.class;
    public boolean setNavigatiobBarColor() default false;
    public int menuId() default 0;
    public String toolbarName() default "toolbar";
}
