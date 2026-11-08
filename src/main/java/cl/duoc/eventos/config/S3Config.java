package cl.duoc.eventos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    /**
     * Sin llaves en el código: el SDK usa su cadena de credenciales por defecto.
     * En la EC2 las toma del rol de la instancia (LabInstanceProfile); en local, de variables AWS_*.
     */
    @Bean
    public S3Client s3Client(@Value("${app.aws.region}") String region) {
        return S3Client.builder().region(Region.of(region)).build();
    }
}
