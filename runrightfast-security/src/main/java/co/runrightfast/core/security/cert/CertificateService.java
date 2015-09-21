/*
 Copyright 2015 Alfio Zappala

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package co.runrightfast.core.security.cert;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 *
 * @author alfio
 */
public interface CertificateService {

    X509Certificate generateX509CertificateV1(X509V1CertRequest request, PrivateKey privateKey) throws CertificateServiceException;
}
