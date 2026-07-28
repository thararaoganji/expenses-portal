package portal.expenses.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.UUID;

public class PasswordEncoderUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoderUtil.class);

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Get passwords from args or generate random ones to avoid hardcoded credentials
        String employeePassword = args.length > 0 ? args[0] : "emp_" + UUID.randomUUID().toString().substring(0, 8);
        String managerPassword = args.length > 1 ? args[1] : "mgr_" + UUID.randomUUID().toString().substring(0, 8);
        String financePassword = args.length > 2 ? args[2] : "fin_" + UUID.randomUUID().toString().substring(0, 8);

        if (logger.isInfoEnabled()) {
            logger.info("Employee Password ('{}'): {}", employeePassword, encoder.encode(employeePassword));
            logger.info("Manager Password ('{}'): {}", managerPassword, encoder.encode(managerPassword));
            logger.info("Finance Password ('{}'): {}", financePassword, encoder.encode(financePassword));
        }
    }
}
