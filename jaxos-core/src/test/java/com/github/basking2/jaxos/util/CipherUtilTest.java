package com.github.basking2.jaxos.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import com.github.basking2.jaxos.JaxosConfiguration;
import org.junit.Test;

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
