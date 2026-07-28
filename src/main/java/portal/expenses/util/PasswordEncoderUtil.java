package portal.expenses.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoderUtil.class);

    private PasswordEncoderUtil() {}

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        if (args.length >= 3 && logger.isInfoEnabled()) {
            logger.info("Employee Hash: {}", encoder.encode(args[0]));
            logger.info("Manager Hash: {}", encoder.encode(args[1]));
            logger.info("Finance Hash: {}", encoder.encode(args[2]));
        }
    }
}
