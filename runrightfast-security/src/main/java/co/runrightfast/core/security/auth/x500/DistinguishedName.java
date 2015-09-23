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
package co.runrightfast.core.security.auth.x500;

import static com.google.common.base.Preconditions.checkArgument;
import java.util.Arrays;
import javax.security.auth.x500.X500Principal;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.x500.X500Name;

/**
 * <pre>
 * RFC 2253               LADPv3 Distinguished Names          December 1997
 *
 *
 *                    String  X.500 AttributeType
 *                    ------------------------------
 *                    CN      commonName
 *                    L       localityName
 *                    ST      stateOrProvinceName
 *                    O       organizationName
 *                    OU      organizationalUnitName
 *                    C       countryName
 *                    STREET  streetAddress
 *                    DC      domainComponent
 *                    UID     userid
 * </pre>
 *
 * @author alfio
 */
@Builder
@Data
public class DistinguishedName {

    /**
     * CN
     *
     * max length = 64
     */
    private final String commonName;

    /**
     * L
     *
     * max length = 64
     */
    private final String localityName;

    /**
     * ST
     *
     * max length = 64
     */
    private final String stateOrProvinceName;

    /**
     * O
     *
     * max length = 64
     */
    private final String organizationName;

    /**
     * OU
     *
     * max length = 64
     */
    private final String organizationalUnitName;

    /**
     * C
     *
     * max length = 2
     */
    private final String country;

    /**
     * STREET
     *
     * max length = 64
     */
    private final String streetAddress;

    /**
     * DC
     *
     * max length = 64
     *
     * e.g., "www.runrightfast.co " will be converted to DC=www,DC=runrightfast,DC=co
     */
    private final String domain;

    /**
     * UID
     *
     * max length = 64
     */
    private final String userid;

    public DistinguishedName(String commonName, String localityName, String stateOrProvinceName, String organizationName, String organizationalUnitName, String country, String streetAddress, String domain, String userid) {
        this.commonName = StringUtils.trim(commonName);
        this.localityName = StringUtils.trim(localityName);
        this.stateOrProvinceName = StringUtils.trim(stateOrProvinceName);
        this.organizationName = StringUtils.trim(organizationName);
        this.organizationalUnitName = StringUtils.trim(organizationalUnitName);
        this.country = StringUtils.trim(country);
        this.streetAddress = StringUtils.trim(streetAddress);
        this.domain = StringUtils.trim(domain);
        this.userid = StringUtils.trim(userid);

        validate();
    }

    private void validate() {
        validateAttributeValue(commonName, 64);
        validateAttributeValue(localityName, 64);
        validateAttributeValue(stateOrProvinceName, 64);
        validateAttributeValue(organizationName, 64);
        validateAttributeValue(organizationalUnitName, 64);
        validateAttributeValue(country, 2);
        validateAttributeValue(streetAddress, 64);
        if (StringUtils.isNotBlank(domain)) {
            validateAttributeValue(domain, 64);
        }
    }

    /**
     *
     * @return String DN in RFC 2253 format
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(128);
        if (StringUtils.isNotBlank(userid)) {
            sb.append("UID=").append(userid).append(',');
        }
        if (StringUtils.isNotBlank(commonName)) {
            sb.append("CN=").append(commonName).append(',');
        }
        if (StringUtils.isNotBlank(organizationalUnitName)) {
            sb.append("OU=").append(organizationalUnitName).append(',');
        }
        if (StringUtils.isNotBlank(organizationName)) {
            sb.append("O=").append(organizationName).append(',');
        }
        if (StringUtils.isNotBlank(country)) {
            sb.append("C=").append(country).append(',');
        }
        if (StringUtils.isNotBlank(stateOrProvinceName)) {
            sb.append("ST=").append(stateOrProvinceName).append(',');
        }
        if (StringUtils.isNotBlank(streetAddress)) {
            sb.append("STREET=").append(streetAddress).append(',');
        }
        if (StringUtils.isNotBlank(country)) {
            sb.append("C=").append(country).append(',');
        }
        if (StringUtils.isNotBlank(domain)) {
            Arrays.stream(StringUtils.split(domain, '.')).forEach(domainComponent -> sb.append("DC=").append(domainComponent).append(','));
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    private void validateAttributeValue(final String value, int maxLength) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        checkArgument(!Character.isWhitespace(value.charAt(0)), "cannot start with whitespace");
        checkArgument(value.charAt(0) != '#', "cannot start with whitespace");

        final char[] invalidChars = {',', '+', '"', '\\', '<', '>', ';'};
        final String invalidCharMessage = "invalid char found '%s' within : %s";
        for (final char c : invalidChars) {
            final int index = value.indexOf(c);
            if (index != -1) {
                checkArgument(index != 0, invalidCharMessage, c, value);
                checkArgument(value.charAt(index - 1) == '\\', invalidCharMessage, c, value);
            }
        }
    }

    public X500Principal toX500Principal() {
        return new X500Principal(toString());
    }

    public static X500Name toX500Name(@NonNull final X500Principal principal) {
        return X500Name.getInstance(principal.getEncoded());
    }

}
