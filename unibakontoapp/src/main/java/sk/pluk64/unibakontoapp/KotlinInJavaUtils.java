package sk.pluk64.unibakontoapp;

import android.support.v4.util.Consumer;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class KotlinInJavaUtils {
    public static <T> Function1<T, Unit> fromConsumer(Consumer<T> callable) {
        return t -> {
            callable.accept(t);
            return Unit.INSTANCE;
        };
    }
}
