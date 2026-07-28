package portal.expenses.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoderUtil.class);

    private PasswordEncoderUtil() {
        // Utility class
    }

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String employeePassword = args.length > 0 ? args[0] : "empPass_" + System.currentTimeMillis();
        String managerPassword = args.length > 1 ? args[1] : "mgrPass_" + System.currentTimeMillis();
        String financePassword = args.length > 2 ? args[2] : "finPass_" + System.currentTimeMillis();

        if (logger.isInfoEnabled()) {
            logger.info("Employee Password Hash: {}", encoder.encode(employeePassword));
            logger.info("Manager Password Hash: {}", encoder.encode(managerPassword));
            logger.info("Finance Password Hash: {}", encoder.encode(financePassword));
        }
    }
}
