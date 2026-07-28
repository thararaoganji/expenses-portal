package portal.expenses.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoderUtil.class);

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // --- Generate hashes dynamically for initial users ---
        String employeePassword = args.length > 0 ? args[0] : java.util.UUID.randomUUID().toString();
        String managerPassword = args.length > 1 ? args[1] : java.util.UUID.randomUUID().toString();
        String financePassword = args.length > 2 ? args[2] : java.util.UUID.randomUUID().toString();

        if (logger.isInfoEnabled()) {
            logger.info("Employee Password ('{}'): {}", employeePassword, encoder.encode(employeePassword));
            logger.info("Manager Password ('{}'): {}", managerPassword, encoder.encode(managerPassword));
            logger.info("Finance Password ('{}'): {}", financePassword, encoder.encode(financePassword));
        }
    }
}
