package org.sdsai.jaxos.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.sdsai.jaxos.JaxosConfiguration;

/**
 */
public class CipherUtilTest {
    @Test
    public void testEncrypt() throws Exception {
        final CipherUtil p = new CipherUtil(new JaxosConfiguration());

        final byte[] plaintext = "Hi!".getBytes();
        final byte[] ciphertext = p.encrypt(plaintext);
        final byte[] result = p.decrypt(ciphertext);

        assertArrayEquals(plaintext, result);
        assertThat(plaintext, not(equalTo(ciphertext)));

    }
}
