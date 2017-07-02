package de.uni_oldenburg.transport.optimizers;

import de.uni_oldenburg.transport.*;
import de.uni_oldenburg.transport.trucks.LargeTruck;
import de.uni_oldenburg.transport.trucks.MediumTruck;
import de.uni_oldenburg.transport.trucks.SmallTruck;

import java.util.*;


/**
 * Optimizer with Savings Algorithm
 */
public class SavingsOptimizer implements Optimizer {

    Location startLocation;

    @Override
    public Solution optimizeTransportNetwork(TransportNetwork transportNetwork) {
        startLocation = transportNetwork.getStartLocation();
        Solution solution = new Solution(transportNetwork);

        HashMap<Location, Integer> shortestWays = depotDistance(transportNetwork);
        LinkedHashMap<String, Integer> savings = getSavings(shortestWays, transportNetwork);

        ArrayList<ArrayList<Location>> getRoutes = getRoutes(savings, transportNetwork);
        ArrayList<Tour> tours = null;
        ArrayList<ArrayList<Tour>> allTours = new ArrayList<>();


        for (int i = 0; i < getRoutes.size(); i++) {
            ArrayList<Location> tourLocations = getRoutes.get(i);
            tours = new ArrayList<>();
            int totalAmount = 0;
            for (Location tourDestination : tourLocations) {
                totalAmount += tourDestination.getAmount();
            }
            int totalsAmountCopy = totalAmount;
            while (totalsAmountCopy > 0) {
                if (totalsAmountCopy > MediumTruck.CAPACITY && totalsAmountCopy <= LargeTruck.CAPACITY) {
                    tours.add(new Tour(new LargeTruck(), startLocation));
                    totalsAmountCopy -= LargeTruck.CAPACITY;
                } else if (totalsAmountCopy > SmallTruck.CAPACITY && totalsAmountCopy <= MediumTruck.CAPACITY) {
                    tours.add(new Tour(new MediumTruck(), startLocation));
                    totalsAmountCopy -= MediumTruck.CAPACITY;
                } else if (totalsAmountCopy <= SmallTruck.CAPACITY) {
                    tours.add(new Tour(new SmallTruck(), startLocation));
                    totalsAmountCopy -= SmallTruck.CAPACITY;
                } else {

                    do {
                        tours.add(new Tour(new LargeTruck(), startLocation));
                        totalsAmountCopy -= LargeTruck.CAPACITY;
                    } while (totalsAmountCopy > MediumTruck.CAPACITY);
                }
            }

            for (int j = 0; j < tours.size(); j++) {
                int amountPossible = tours.get(j).getTruck().getCapacity();
                Location startTourLocation = startLocation;
                Tour tour = tours.get(j);
                int expense = 0;
                int unL = 0;
                for (int k = 0; k < tourLocations.size(); k++) {
                    if (amountPossible > 0) {
                        Location actLocation = tourLocations.get(k);
                        int unload = 0;
                        int unloadAmount = 0;
                        expense = getExpense(shortestWays, startTourLocation, actLocation);
                        TourDestination tourDestination = new TourDestination(actLocation, tourLocations.get(k).getAmount());
                        unload = tourDestination.getUnload();
                        if (unload <= amountPossible) {
                            tourDestination.setUnload(actLocation.getAmount());
                            tourLocations.get(k).setAmount(0);
                            unloadAmount = unload;
                        } else {
                            int restAmount = tourLocations.get(k).getAmount() - amountPossible;
                            unloadAmount = amountPossible;
                            tourDestination.setUnload(unloadAmount);
                            tourLocations.get(k).setAmount(restAmount);
                        }
                        tour.addDestination(tourDestination);
                        amountPossible -= unload;

                        // System.out.println("LKW " + j + " drives " + expense + " kilometers from " + startTourLocation.getName() + " to " + actLocation.getName() + " and unloads " + unloadAmount + " at tour number " + j);

                        startTourLocation = actLocation;
                    } else if (k < tourLocations.size()) {
                        Location locNoLoad = tourLocations.get(k);

                        TourDestination destNoLoad = new TourDestination(locNoLoad, unL);
                        tour.addDestination(destNoLoad);

                    }
                }

            }

            allTours.add(tours);


        }
        for (int i = 0; i < allTours.size(); i++) {
            for (int j = 0; j < allTours.get(i).size(); j++) {
                if (allTours.get(i).get(j).isValid())
                    solution.addTour(allTours.get(i).get(j));
            }
        }

        return solution;

    }

    /**
     * Gets the savings for the distances between the Locations and the Depot
     *
     * @param depotDistance    HashMap with the distances to the depot
     * @param transportNetwork
     * @return
     */
    private LinkedHashMap<String, Integer> getSavings(HashMap<Location, Integer> depotDistance, TransportNetwork transportNetwork) {
        ArrayList<Location> visitedLocations = new ArrayList<>();
        Location actLocation = startLocation;
        LinkedHashMap<String, Integer> savings = new LinkedHashMap<>();

        while (transportNetwork.getNumberOfLocations() > visitedLocations.size()) {
            int distance = 0;
            Location location = null;
            visitedLocations.add(startLocation);
            String names = "";
            int d1 = 0;
            int d2 = 0;
            int save = 0;
            for (int i = 0; i < visitedLocations.size(); i++) {
                if (!actLocation.getNeighbouringLocations().isEmpty()) {
                    for (Map.Entry<Location, Integer> entry : actLocation.getNeighbouringLocations().entrySet()) {
                        distance = entry.getValue();
                        location = entry.getKey();

                        for (Map.Entry<Location, Integer> dis : depotDistance.entrySet()) {
                            Location locationDis = dis.getKey();

                            if (actLocation.toString().equals(locationDis.toString())) {
                                d1 = dis.getValue();
                                names += locationDis.getName();
                            }
                            if (location.toString().equals(locationDis.toString())) {
                                d2 = dis.getValue();
                                names += location.getName();
                            }
                            save = d1 + d2 - distance;
                        }

                        if (!visitedLocations.contains(location)) {
                            visitedLocations.add(location);
                            distance = 0;
                        }

                        if (!names.contains("Hamburg")) {
                            savings.put(names, save);
                        }


                        names = "";
                    }
                }
                actLocation = visitedLocations.get(i);
            }
        }
        return savings;
    }

    /**
     * Computes the routes with multiple destinations.
     *
     * @param savings          HashMap with each saving for two locations
     * @param transportNetwork
     * @return
     */
    private ArrayList<ArrayList<Location>> getRoutes(LinkedHashMap<String, Integer> savings, TransportNetwork transportNetwork) {

        ArrayList<ArrayList<Location>> routeList = new ArrayList<>();
        ArrayList<Location> routeLocation = new ArrayList<>();
        ArrayList<Location> visitedLocations = new ArrayList<>();
        ArrayList<Location> locations = new ArrayList<>();
        ArrayList<Object> sortedSav = new ArrayList<>();
        ArrayList<Location> delete = new ArrayList<>();
        ArrayList<ArrayList<Location>> deleteList = new ArrayList<>();
        boolean[] savingsCheck = new boolean[savings.size()];

        LinkedHashMap<String, Integer> sorted = savings;

        Location actLocation = startLocation;
        routeLocation.add(startLocation);

        Object[] o = sorted.entrySet().toArray();
        Arrays.sort(o, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Map.Entry<String, Integer>) o2).getValue()
                        .compareTo(((Map.Entry<String, Integer>) o1).getValue());
            }
        });
        System.out.println("Savings: ");
        for (Object e : o) {
            sortedSav.add(e);
            sorted.put(((Map.Entry<String, Integer>) e).getKey(), ((Map.Entry<String, Integer>) e).getValue());
            System.out.println(((Map.Entry<String, Integer>) e).getKey() + " : "
                    + ((Map.Entry<String, Integer>) e).getValue());
        }

        System.out.println();

        for (int i = 0; i < transportNetwork.getLocations().length; i++) {
            locations.add(transportNetwork.getLocations()[i]);
        }
        int k = 0;
        int checkCounter = 0;
        while (k < 1) {
            for (Object e : o) {
                ArrayList<Location> savingsLocations = new ArrayList<>();
                for (Location loc : locations) {
                    if (((Map.Entry<String, Integer>) e).getKey().contains(loc.getName())) {
                        if (!savingsLocations.contains(loc)) {
                            savingsLocations.add(loc);
                        }
                    }
                }

                ArrayList<Location> singleRoute = new ArrayList<>();
                transportNetwork.computeShortestPaths();
                ArrayList<Location> firstRoute = new ArrayList<>();

                if (routeList.size() == 0) {
                    LinkedHashMap<Location, Integer> route = transportNetwork.getShortestPath(startLocation, savingsLocations.get(0));

                    for (Map.Entry<Location, Integer> entry : route.entrySet()) {
                        if (!entry.getKey().equals(startLocation))
                            firstRoute.add(entry.getKey());
                        if (!visitedLocations.contains(entry.getKey())) {
                            visitedLocations.add(entry.getKey());
                        }
                    }

                    LinkedHashMap<Location, Integer> route2 = transportNetwork.getShortestPath(savingsLocations.get(1), startLocation);

                    for (Map.Entry<Location, Integer> entry : route2.entrySet()) {
                        firstRoute.add(entry.getKey());
                        if (!visitedLocations.contains(entry.getKey())) {
                            visitedLocations.add(entry.getKey());
                        }
                    }
                    routeList.add(firstRoute);
                    savingsCheck[checkCounter] = true;

                    k++;

                } else {
                    boolean checkSaving = true;
                    for (ArrayList<Location> list : routeList) {

                        if (list.contains(savingsLocations.get(0)) && list.contains(savingsLocations.get(1))) {
                            System.out.println("Saving ist bereits in Tour eingeplant.");
                            checkSaving = false;

                        } else if (list.contains(savingsLocations.get(1))) {
                            for (ArrayList<Location> list2 : routeList) {
                                if (list2.contains(savingsLocations.get(0))) {
                                    System.out.println("Saving ist bereits in Tour eingeplant 1.");
                                    checkSaving = false;
                                }
                            }
                        } else if (list.contains(savingsLocations.get(0))) {
                            for (ArrayList<Location> list2 : routeList) {
                                if (list2.contains(savingsLocations.get(1))) {
                                    System.out.println("Saving ist bereits in Tour eingeplant 2.");
                                    checkSaving = false;
                                }
                            }
                        } else {
                            checkSaving = true;
                        }
                    }

                    Iterator<ArrayList<Location>> iter = routeList.iterator();

                    while (iter.hasNext()) {
                        ArrayList<Location> list = iter.next();


                        if (list.contains(savingsLocations.get(0)) && list.contains(savingsLocations.get(1))) {
                            System.out.println("Saving ist bereits in Tour eingeplant.");

                            break;
                        } else if (checkSaving == true) {
                            if (list.contains(savingsLocations.get(1))) {
                                for (ArrayList<Location> list2 : routeList) {
                                    if (list2.contains(savingsLocations.get(0))) {
                                        System.out.println("Saving ist bereits in Tour eingeplant 1.");
                                        break;
                                    }
                                }
                                LinkedHashMap<Location, Integer> route = transportNetwork.getShortestPath(startLocation, savingsLocations.get(0));
                                ArrayList<Location> routePart = new ArrayList<>();
                                for (Map.Entry<Location, Integer> entry : route.entrySet()) {
                                    if (!entry.getKey().equals(startLocation))
                                        singleRoute.add(entry.getKey());
                                    routePart.add(entry.getKey());
                                    if (!visitedLocations.contains(entry.getKey())) {
                                        visitedLocations.add(entry.getKey());
                                    }
                                }

                                int count = singleRoute.size();

                                while (count < list.size() + 1) {
                                    singleRoute.add(list.get(count - 1));
                                    count++;
                                }
                                for (Location destination2 : list) {
                                    if (!singleRoute.contains(destination2)) {
                                        singleRoute.add(destination2);
                                    }
                                }
                                savingsCheck[checkCounter] = true;
                                routeList.remove(list);
                                break;
                            } else if (list.contains(savingsLocations.get(0))) {
                                for (ArrayList<Location> list2 : routeList) {
                                    if (list2.contains(savingsLocations.get(1))) {
                                        System.out.println("Saving ist bereits in Tour eingeplant 2.");
                                        break;
                                    }
                                }
                                LinkedHashMap<Location, Integer> route2 = transportNetwork.getShortestPath(startLocation, savingsLocations.get(1));
                                ArrayList<Location> routePart = new ArrayList<>();
                                for (Map.Entry<Location, Integer> entry : route2.entrySet()) {
                                    routePart.add(entry.getKey());
                                    if (!visitedLocations.contains(entry.getKey())) {
                                        visitedLocations.add(entry.getKey());
                                    }
                                }
                                int count = 0;
                                for (int i = 0; i < routePart.size(); i++) {
                                    if (!routePart.get(i).equals(startLocation)) {
                                        if (routePart.get(i).equals(list.get(i))) {
                                            singleRoute.add(list.get(i));
                                            count++;
                                        } else {
                                            singleRoute.add(routePart.get(i));
                                            count++;
                                        }
                                    }
                                }
                                while (count < list.size() + 1) {
                                    singleRoute.add(list.get(count - 1));
                                    count++;
                                }
                                routeList.remove(list);
                                savingsCheck[checkCounter] = true;
                                break;
                            } else {
                                LinkedHashMap<Location, Integer> route = transportNetwork.getShortestPath(startLocation, savingsLocations.get(0));

                                for (Map.Entry<Location, Integer> entry : route.entrySet()) {
                                    if (!entry.getKey().equals(startLocation)) {
                                        if (!singleRoute.contains(entry.getKey())) {
                                            singleRoute.add(entry.getKey());
                                        }
                                    }
                                    if (!visitedLocations.contains(entry.getKey())) {
                                        visitedLocations.add(entry.getKey());
                                    }
                                }

                                LinkedHashMap<Location, Integer> route2 = transportNetwork.getShortestPath(savingsLocations.get(1), startLocation);

                                for (Map.Entry<Location, Integer> entry : route2.entrySet()) {
                                    if (!entry.getKey().equals(visitedLocations.get(visitedLocations.size() - 1))) {
                                        singleRoute.add(entry.getKey());
                                    }
                                    if (!visitedLocations.contains(entry.getKey())) {
                                        visitedLocations.add(entry.getKey());
                                    }
                                }
                                savingsCheck[checkCounter] = true;
                                break;
                            }
                        }
                    }
                }

                boolean check = false;

                for (int g = 0; g < singleRoute.size(); g++) {
                    for (Map.Entry<Location, Integer> neighbour : singleRoute.get(g).getNeighbouringLocations().entrySet()) {
                        if (g + 1 < singleRoute.size())
                            if (neighbour.getKey().equals(singleRoute.get(g + 1))) {
                                check = true;
                            }
                    }

                }

                if (singleRoute.size() != 0 && check == true) {
                    routeList.add(singleRoute);
                }

                deleteList.add(delete);
                k++;
                checkCounter++;
            }
        }

        for (Location visitedL : locations) {
            if (!visitedLocations.contains(visitedL)) {
                LinkedHashMap<Location, Integer> route = transportNetwork.getShortestPath(startLocation, visitedL);

                for (Map.Entry<Location, Integer> entry : route.entrySet()) {
                    if (!startLocation.equals(entry.getKey())) {
                        if (!routeLocation.contains(entry.getKey())) {
                            routeLocation.add(entry.getKey());
                        }
                    }
                    if (!visitedLocations.contains(entry.getKey())) {
                        visitedLocations.add(entry.getKey());
                    }
                }

                LinkedHashMap<Location, Integer> route2 = transportNetwork.getShortestPath(visitedL, startLocation);

                for (Map.Entry<Location, Integer> entry : route2.entrySet()) {
                    if (!entry.getKey().equals(visitedLocations.get(visitedLocations.size() - 1))) {
                        routeLocation.add(entry.getKey());
                    }
                    if (!visitedLocations.contains(entry.getKey())) {
                        visitedLocations.add(entry.getKey());
                    }
                }
            }
        }

        // routeList.add(routeLocation);

        for (ArrayList<Location> remove : deleteList) {
            routeList.remove(remove);
        }
        System.out.println("Ergebnisse saving:");
        for (boolean b : savingsCheck) {
            System.out.println(b);
        }


        System.out.println("Anzahl Touren: " + routeList.size());
        for (int j = 0; j < routeList.size(); j++) {
            ArrayList<Location> route1 = routeList.get(j);
            System.out.print("Route mit " + route1.size() + " destinations: ");
            for (int i = 0; i < route1.size(); i++) {
                System.out.print(route1.get(i).getName() + " ");
            }
            System.out.println();
        }
        System.out.println();

        System.out.println("Besuchte Locations: ");
        for (int h = 0; h < visitedLocations.size(); h++)
            System.out.println(visitedLocations.get(h));


        return routeList;
    }


    /**
     * Finds out the shortest way from the startLocation to each Location.
     *
     * @param network
     * @return
     */
    private HashMap depotDistance(TransportNetwork network) {
        HashMap<Location, Integer> depotDistanceHashMap = new HashMap<>();
        ArrayList<Location> visitedLocations = new ArrayList<>();
        Location actLocation = startLocation;
        depotDistanceHashMap.put(startLocation, 0);

        while (network.getNumberOfLocations() > visitedLocations.size()) {
            int distance = 0;
            Location location = null;
            visitedLocations.add(startLocation);
            for (int i = 0; i < visitedLocations.size(); i++) {
                if (!actLocation.getNeighbouringLocations().isEmpty()) {
                    for (Map.Entry<Location, Integer> entry : actLocation.getNeighbouringLocations().entrySet()) {
                        distance = entry.getValue();
                        location = entry.getKey();
                        for (Map.Entry<Location, Integer> dis : depotDistanceHashMap.entrySet()) {
                            Location locationDis = dis.getKey();
                            int dist = dis.getValue();
                            if (actLocation.toString().equals(locationDis.toString())) {
                                distance = distance + dist;
                            }
                        }
                        if (!depotDistanceHashMap.containsKey(location))
                            depotDistanceHashMap.put(location, distance);
                        if (!visitedLocations.contains(location)) {
                            visitedLocations.add(location);
                            distance = 0;
                        }
                    }
                }
                actLocation = visitedLocations.get(i);
            }
        }
        System.out.println("Entfernungen zum Depot: ");
        for (Map.Entry<Location, Integer> entry : depotDistanceHashMap.entrySet()) {
            System.out.println(entry.getKey().getName() + " ist " + entry.getValue() + " km von Hamburg entfernt.");
        }
        System.out.println();
        return depotDistanceHashMap;
    }

    /**
     * @param shortestWays
     * @param location
     * @return
     */
    private int getExpense(HashMap<Location, Integer> shortestWays, Location startLocation, Location location) {
        int expense = 0;

        if (startLocation.equals(this.startLocation)) {
            for (Map.Entry<Location, Integer> entry : shortestWays.entrySet()) {
                if (entry.getKey().equals(location)) {
                    expense = entry.getValue();
                }
            }
        } else if (location.equals(this.startLocation)) {
            for (Map.Entry<Location, Integer> entry : shortestWays.entrySet()) {
                if (entry.getKey().equals(startLocation)) {
                    expense = entry.getValue();
                }
            }
        } else {
            for (Map.Entry<Location, Integer> neighbour : startLocation.getNeighbouringLocations().entrySet()) {
                if (neighbour.getKey().equals(location)) {
                    expense = neighbour.getValue();
                }
            }
        }

        return expense;
    }


}

