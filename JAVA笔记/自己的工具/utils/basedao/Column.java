package cn.zhu.utils.basedao;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/*
 * 用来标识除了ID以外的column的bean注解，只能用在bean的field上面
 * 标识除了ID以外的数据库对应的列名
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Column {
    String value();
}
