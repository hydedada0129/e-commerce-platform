package demo.bigwork.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingUtilTest {

    @Test
    void maskPhone_longNumber_keepsFirst4AndLast2() {
        assertThat(MaskingUtil.maskPhone("0912345678")).isEqualTo("0912****78");
    }

    @Test
    void maskPhone_shortNumber_masksEntirely() {
        assertThat(MaskingUtil.maskPhone("12345")).isEqualTo("*****");
    }

    @Test
    void maskPhone_null_returnsNull() {
        assertThat(MaskingUtil.maskPhone(null)).isNull();
    }

    @Test
    void maskAddress_longAddress_keepsFirst3Chars() {
        assertThat(MaskingUtil.maskAddress("台北市信義區")).isEqualTo("台北市****");
    }

    @Test
    void maskAddress_shortAddress_masksEntirely() {
        assertThat(MaskingUtil.maskAddress("TW")).isEqualTo("**");
    }

    @Test
    void maskAddress_null_returnsNull() {
        assertThat(MaskingUtil.maskAddress(null)).isNull();
    }
}
