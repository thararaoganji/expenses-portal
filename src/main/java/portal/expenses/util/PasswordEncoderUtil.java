package portal.expenses.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoderUtil.class);

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String employeePassword = args.length > 0 ? args[0] : "emp-" + java.util.UUID.randomUUID();
        String managerPassword = args.length > 1 ? args[1] : "mgr-" + java.util.UUID.randomUUID();
        String financePassword = args.length > 2 ? args[2] : "fin-" + java.util.UUID.randomUUID();

        if (logger.isInfoEnabled()) {
            logger.info("Employee Password: {}", encoder.encode(employeePassword));
            logger.info("Manager Password: {}", encoder.encode(managerPassword));
            logger.info("Finance Password: {}", encoder.encode(financePassword));
        }
    }
}
