package portal.expenses.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoderUtil.class);

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String emp = args.length > 0 ? args[0] : "empPass123!";
        String mgr = args.length > 1 ? args[1] : "mgrPass123!";
        String fin = args.length > 2 ? args[2] : "finPass123!";

        if (logger.isInfoEnabled()) {
            logger.info("Employee Hash: {}", encoder.encode(emp));
            logger.info("Manager Hash: {}", encoder.encode(mgr));
            logger.info("Finance Hash: {}", encoder.encode(fin));
        }
    }
}
