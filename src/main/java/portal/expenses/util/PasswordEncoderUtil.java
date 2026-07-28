package portal.expenses.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoderUtil.class);

    @SuppressWarnings("java:S6437") // Hardcoded strings are only for local utility hash generation, not used in production
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // --- Generate hashes for your initial users ---
        String employeePassword = "password-employee";
        String managerPassword = "password-manager";
        String financePassword = "password-finance";

        if (logger.isInfoEnabled()) {
            logger.info("Employee Password ('{}'): {}", employeePassword, encoder.encode(employeePassword));
            logger.info("Manager Password ('{}'): {}", managerPassword, encoder.encode(managerPassword));
            logger.info("Finance Password ('{}'): {}", financePassword, encoder.encode(financePassword));
        }
    }
}
