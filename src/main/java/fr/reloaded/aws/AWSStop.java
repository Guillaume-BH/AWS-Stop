package fr.reloaded.aws;

import org.bukkit.plugin.java.JavaPlugin;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AWSStop extends JavaPlugin {

    private Ec2Client ec2;

    @Override
    public void onDisable() {
        String accessKeyId = this.getConfig().getString("AWS_ACCESS_KEY_ID");
        String secretAccessKey = this.getConfig().getString("AWS_SECRET_ACCESS_KEY");
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        this.ec2 = Ec2Client.builder()
                .region(Region.EU_WEST_3)
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
        // Get public IP of the instance
        String ip;
        try {
            ip = this.getPublicAddress();
            String instanceId = this.getInstanceIdByIp(ip);
            if (instanceId != null) {
                TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder()
                        .instanceIds(instanceId)
                        .build();
                ec2.terminateInstances(terminateRequest);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getPublicAddress() throws IOException {
        URL url = new URL("http://checkip.amazonaws.com/");
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

        String publicIp = in.readLine(); // IP returned by AWS service
        in.close();

        if (publicIp != null && !publicIp.isEmpty()) {
            return publicIp;
        } else {
            throw new IllegalStateException("Unable to retrieve public IP address");
        }
    }


    public String getInstanceIdByIp(String ip) {
        for (Instance runningInstance : this.getRunningInstances()) {
            if (runningInstance.publicIpAddress().equals(ip)) {
                return runningInstance.instanceId();
            }
        }
        return null;
    }

    private List<Instance> getRunningInstances() {

        DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();
        DescribeInstancesResponse response = ec2.describeInstances(request);

        List<Instance> instances = response.reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .collect(Collectors.toList()), runningInstances = new ArrayList<>();

        for (Instance instance : instances) {
            if (instance.state().nameAsString().equalsIgnoreCase("running")) {
                runningInstances.add(instance);
            }
        }
        return runningInstances;
    }
}
