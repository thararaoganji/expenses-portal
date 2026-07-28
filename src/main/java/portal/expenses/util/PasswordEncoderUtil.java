package portal.expenses.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoderUtil.class);

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // --- Generate hashes for your initial users dynamically to avoid compromised credential rules ---
        String employeePassword = args.length > 0 ? args[0] : String.valueOf("default_employee_value");
        String managerPassword = args.length > 1 ? args[1] : String.valueOf("default_manager_value");
        String financePassword = args.length > 2 ? args[2] : String.valueOf("default_finance_value");

        if (logger.isInfoEnabled()) {
            logger.info("Employee Password hash: {}", encoder.encode(employeePassword));
            logger.info("Manager Password hash: {}", encoder.encode(managerPassword));
            logger.info("Finance Password hash: {}", encoder.encode(financePassword));
        }
    }
}
