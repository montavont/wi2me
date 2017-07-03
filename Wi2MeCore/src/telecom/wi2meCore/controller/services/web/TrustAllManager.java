/**
 * Copyright (c) 2012 Institut Mines-Telecom / Telecom Bretagne. All rights reserved.
 *
 * This file is part of Wi2Me.
 *
 * Wi2Me is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wi2Me is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wi2Me.  If not, see <http://www.gnu.org/licenses/>.
 */

package telecom.wi2meCore.controller.services.web;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * This X509TrustManager trust only the certificate pass to the constructor
 * @author Julien Mortuaire
 */
public class TrustAllManager implements X509TrustManager {
	private String publicKey;
	private String commonName;
	
	/**
	 * TrustAllManager constructor
	 * @param publicKey the public key
	 * @param commonName the commonName (CN)
	 */
	public TrustAllManager(String publicKey, String commonName){
		this.publicKey = publicKey;
		this.commonName = "CN="+commonName;
	}
	
    /* (non-Javadoc)
     * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.X509Certificate[], java.lang.String)
     */
    public void checkClientTrusted(X509Certificate[] cert, String authType) throws CertificateException {
    }
    /* (non-Javadoc)
     * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[], java.lang.String)
     */
    public void checkServerTrusted(X509Certificate[] cert, String authType) throws CertificateException {
        for (X509Certificate x : cert) {
		if ((x.getSubjectDN()!=null) && (x.getSubjectDN().getName() !=null))
        	{
        		if (x.getPublicKey().toString().contains(publicKey))
        			return;
		}
		System.out.println("Did not find our public key in the certificate, we are probably uotdated.");
        }
        throw new CertificateException("Certifcate not correct");
    }
    /* (non-Javadoc)
     * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
     */
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
