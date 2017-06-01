package de.uni_oldenburg.transport;

import de.uni_oldenburg.transport.csv.DeliveryCSVLoader;
import de.uni_oldenburg.transport.csv.TransportNetworkCSVLoader;
import de.uni_oldenburg.transport.optimizers.GeneticOptimizer;
import de.uni_oldenburg.transport.optimizers.NearestNeighborOptimizer;
import de.uni_oldenburg.transport.optimizers.Optimizer;

/**
 * This class is the entry point to our optimizers
 */
public class AppStarter {

	/**
	 * The app's main method
	 *
	 * @param args CLI arguments
	 */
	public static void main(String[] args) {

		TransportNetwork transportNetwork = null;
		Optimizer optimizer;
		String networkFile;
		String deliveryFile;
		int optimizerId;

		if (args.length > 0) {
			optimizerId = Integer.parseInt(args[0]);
		} else {
			optimizerId = 0;
		}

		if (args.length > 1) {
			System.out.println("Using custom data files.");
			networkFile = args[1];
			deliveryFile = args[2];
		} else {
			System.out.println("No data files passed. Using default ressources.");
			networkFile = "src/main/resources/Logistiknetz.csv";
			deliveryFile = "src/main/resources/Lieferliste.csv";
		}

		try {

			TransportNetworkCSVLoader transportNetworkCSVLoader = new TransportNetworkCSVLoader(networkFile);
			transportNetwork = transportNetworkCSVLoader.getTransportNetwork();

			DeliveryCSVLoader deliveryCSVLoader = new DeliveryCSVLoader(deliveryFile, transportNetwork);
			transportNetwork = deliveryCSVLoader.getTransportNetworkWithDeliveries();

		} catch (Exception e) {
			System.out.println("Error when reading the input files");
			System.exit(1);
		}

		switch (optimizerId) {
			case 0:
				optimizer = new GeneticOptimizer();
				break;

			case 1:
				optimizer = new NearestNeighborOptimizer();
				break;

			default:
				optimizer = new GeneticOptimizer();
				System.out.println("Optimizer not found");
				System.exit(1);
		}

		Solution solution = optimizer.optimizeTransportNetwork(transportNetwork);

		if (!solution.isValid()) {
			System.out.println("Optimizer did not give a valid solution");
			System.exit(1);
		}

		// Print solution
		System.out.println(solution.toString());
	}

}