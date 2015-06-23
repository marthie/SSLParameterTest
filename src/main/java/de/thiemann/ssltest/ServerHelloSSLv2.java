package de.thiemann.ssltest;

/*
 * This class represents the response of a server which knows $ SSLv2. It
 * includes the list of cipher suites, and the identification of the server
 * certificate.
 * ----------------------------------------------------------------------
 * Copyright (c) 2012  Thomas Pornin <pornin@bolet.org>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ----------------------------------------------------------------------
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class ServerHelloSSLv2 {

	int[] cipherSuites;
	String serverCertName;
	String serverCertHash;

	ServerHelloSSLv2(InputStream in) throws IOException {
		// Record length
		byte[] buf = new byte[2];
		Util.readFully(in, buf);
		int len = Util.dec16be(buf, 0);
		if ((len & 0x8000) == 0) {
			throw new IOException("not a SSLv2 record");
		}
		len &= 0x7FFF;
		if (len < 11) {
			throw new IOException("not a SSLv2 server hello");
		}
		buf = new byte[11];
		Util.readFully(in, buf);
		if (buf[0] != 0x04) {
			throw new IOException("not a SSLv2 server hello");
		}
		int certLen = Util.dec16be(buf, 5);
		int csLen = Util.dec16be(buf, 7);
		int connIdLen = Util.dec16be(buf, 9);
		if (len != 11 + certLen + csLen + connIdLen) {
			throw new IOException("not a SSLv2 server hello");
		}
		if (csLen == 0 || csLen % 3 != 0) {
			throw new IOException("not a SSLv2 server hello");
		}
		byte[] cert = new byte[certLen];
		Util.readFully(in, cert);
		byte[] cs = new byte[csLen];
		Util.readFully(in, cs);
		byte[] connId = new byte[connIdLen];
		Util.readFully(in, connId);
		cipherSuites = new int[csLen / 3];
		for (int i = 0, j = 0; i < csLen; i += 3, j++) {
			cipherSuites[j] = Util.dec24be(cs, i);
		}
		try {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate xc = (X509Certificate) cf
					.generateCertificate(new ByteArrayInputStream(cert));
			serverCertName = xc.getSubjectX500Principal().toString();
			serverCertHash = Util.doSHA1(cert);
		} catch (CertificateException e) {
			// ignored
		}
	}
}