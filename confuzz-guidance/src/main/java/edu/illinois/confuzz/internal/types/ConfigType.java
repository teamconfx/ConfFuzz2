package edu.illinois.confuzz.internal.types;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * Bookkeeping for every config, including type, generated value and such.
 * Also must have static function check.
 */
public interface ConfigType {

    public Object generate(Object value, SourceOfRandomness random);

    public Object getDefault();

    public Object getGenerated();

}
