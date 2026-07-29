package portal.expenses.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoderUtil.class);

    private PasswordEncoderUtil() {
        // Utility class private constructor
    }

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        if (args.length < 3) {
            logger.info("Usage: PasswordEncoderUtil <empPwd> <mgrPwd> <finPwd>");
            return;
        }

        String employeeHash = encoder.encode(args[0]);
        String managerHash = encoder.encode(args[1]);
        String financeHash = encoder.encode(args[2]);

        if (logger.isInfoEnabled()) {
            logger.info("Employee Password Hash: {}", employeeHash);
            logger.info("Manager Password Hash: {}", managerHash);
            logger.info("Finance Password Hash: {}", financeHash);
        }
    }
}
