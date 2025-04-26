package aws.ec2connect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

@Service
public class PricingService {

    private final PricingClient pricingClient;
    private final ObjectMapper objectMapper;


    public PricingService(PricingClient pricingClient) {
       this.pricingClient = pricingClient;
        this.objectMapper = new ObjectMapper();
    }

    public String getPriceForInstanceType(String instanceType, String location) {
        try {
            GetProductsRequest request = GetProductsRequest.builder()
                    .serviceCode("AmazonEC2")
                    .filters(
                            builder -> builder
                                    .field("instanceType")
                                    .type("TERM_MATCH")
                                    .value(instanceType),
                            builder -> builder
                                    .field("location")
                                    .type("TERM_MATCH")
                                    .value(location),
                            builder -> builder
                                    .field("operatingSystem")
                                    .type("TERM_MATCH")
                                    .value("Linux"),
                            builder -> builder
                                    .field("preInstalledSw")
                                    .type("TERM_MATCH")
                                    .value("NA"),
                            builder -> builder
                                    .field("capacitystatus")
                                    .type("TERM_MATCH")
                                    .value("Used")
                    )
                    .maxResults(1)
                    .build();

            GetProductsResponse response = pricingClient.getProducts(request);

            if (!response.priceList().isEmpty()) {
                String priceItemJson = response.priceList().getFirst();

                JsonNode jsonNode = objectMapper.readTree(priceItemJson);
                JsonNode onDemandNode = jsonNode.path("terms").path("OnDemand");
                if (onDemandNode.isMissingNode()) {
                    return "N/A";
                }

                for (JsonNode offer : onDemandNode) {
                    for (JsonNode priceDimension : offer.path("priceDimensions")) {
                        String pricePerUnit = priceDimension.path("pricePerUnit").path("USD").asText();
                        return "$" + pricePerUnit;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "N/A";
    }

    public void close() {
        pricingClient.close();
    }
}