package uk.gov.hmcts.reform.finrem.ccddatamigration.s2s;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AuthUtilTest {

    @Test
    public void shouldGetBearToken() {
        assertThat(AuthUtil.getBearToken("aaaa"), is("Bearer aaaa"));
    }

    @Test
    public void shouldReturnGetBearToken() {
        assertThat(AuthUtil.getBearToken("Bearer aaaa"), is("Bearer aaaa"));
    }

    @Test
    public void shouldReturnBlankToken() {
        assertThat(AuthUtil.getBearToken(""), is(""));
    }
}