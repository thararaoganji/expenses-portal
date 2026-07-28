package portal.expenses.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoderUtil.class);

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // --- Generate hashes for initial users ---
        String empUser = args.length > 0 ? args[0] : "employee_credentials";
        String mgrUser = args.length > 1 ? args[1] : "manager_credentials";
        String finUser = args.length > 2 ? args[2] : "finance_credentials";

        if (logger.isInfoEnabled()) {
            logger.info("Employee Hash: {}", encoder.encode(empUser));
            logger.info("Manager Hash: {}", encoder.encode(mgrUser));
            logger.info("Finance Hash: {}", encoder.encode(finUser));
        }
    }
}
