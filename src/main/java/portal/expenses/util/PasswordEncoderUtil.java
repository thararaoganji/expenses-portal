package portal.expenses.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoderUtil.class);

    public static void main(String[] args) {
        if (args.length < 3) {
            if (logger.isInfoEnabled()) {
                logger.info("Usage: PasswordEncoderUtil <employeePassword> <managerPassword> <financePassword>");
            }
            return;
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String employeePassword = args[0];
        String managerPassword = args[1];
        String financePassword = args[2];

        if (logger.isInfoEnabled()) {
            String empHash = encoder.encode(employeePassword);
            String mgrHash = encoder.encode(managerPassword);
            String finHash = encoder.encode(financePassword);

            logger.info("Employee Password hash: {}", empHash);
            logger.info("Manager Password hash: {}", mgrHash);
            logger.info("Finance Password hash: {}", finHash);
        }
    }
}
