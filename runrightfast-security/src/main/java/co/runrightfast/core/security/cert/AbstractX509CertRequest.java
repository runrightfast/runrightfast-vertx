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

import static co.runrightfast.core.utils.PreconditionErrorMessageTemplates.MUST_BE_AFTER_THAN;
import static co.runrightfast.core.utils.PreconditionErrorMessageTemplates.MUST_BE_GREATER_THAN_ZERO;
import static com.google.common.base.Preconditions.checkArgument;
import java.math.BigInteger;
import java.time.Instant;
import javax.security.auth.x500.X500Principal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * The combination of the {@link AbstractX509CertRequest#issuerPrincipal } {@link AbstractX509CertRequest#serialNumber} must be unique.
 *
 * @author alfio
 */
@ToString
@EqualsAndHashCode(of = {"issuerPrincipal", "serialNumber"})
public abstract class AbstractX509CertRequest {

    @Getter
    protected final X500Principal issuerPrincipal;

    /**
     * Must be a positive number and must be unique per issuerPrincipal
     */
    @Getter
    protected final BigInteger serialNumber;

    @Getter
    protected final Instant notBefore;

    @Getter
    protected final Instant notAfter;

    protected AbstractX509CertRequest(@NonNull final X500Principal issuerPrincipal, @NonNull final BigInteger serialNumber, @NonNull final Instant notBefore, @NonNull final Instant notAfter) {
        checkArgument(serialNumber.signum() == 1, MUST_BE_GREATER_THAN_ZERO, "request.serialNumber");
        checkArgument(notAfter.isAfter(notBefore), MUST_BE_AFTER_THAN, "notAfter", notAfter, "notBefore", notBefore);
        this.issuerPrincipal = issuerPrincipal;
        this.serialNumber = serialNumber;
        this.notBefore = notBefore;
        this.notAfter = notAfter;
    }

}
