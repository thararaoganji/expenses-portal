package portal.expenses.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoderUtil.class);

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Environment or default password placeholders
        String employeePassword = System.getenv().getOrDefault("INIT_EMP_PASS", "changeit-employee");
        String managerPassword = System.getenv().getOrDefault("INIT_MGR_PASS", "changeit-manager");
        String financePassword = System.getenv().getOrDefault("INIT_FIN_PASS", "changeit-finance");

        if (logger.isInfoEnabled()) {
            logger.info("Employee Password hash: {}", encoder.encode(employeePassword));
            logger.info("Manager Password hash: {}", encoder.encode(managerPassword));
            logger.info("Finance Password hash: {}", encoder.encode(financePassword));
        }
    }
}
