package com.backbase.stream;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.FieldReflectionArbitraryIntrospector;
import com.navercorp.fixturemonkey.api.jqwik.JavaTypeArbitraryGenerator;
import com.navercorp.fixturemonkey.api.jqwik.JqwikPlugin;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.arbitraries.StringArbitrary;

public class FixtureUtils {

    public static final FixtureMonkey reflectiveAlphaFixtureMonkey = FixtureMonkey.builder()
        .objectIntrospector(FieldReflectionArbitraryIntrospector.INSTANCE)
        .defaultNotNull(true)
        .plugin(new JqwikPlugin().javaTypeArbitraryGenerator(new JavaTypeArbitraryGenerator() {
            @Override
            public StringArbitrary strings() {
                return Arbitraries.strings().alpha();
            }
        }))
        .build();

}
