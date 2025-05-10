import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;


class Page {
    private int id;
    private boolean isLoaded;
    private int assignedMemory;
    private int address;

    public Page(int id, int assignedMemoryInMB) {
        this.id = id;
        this.isLoaded = false;
        this.assignedMemory = assignedMemoryInMB * 1024 * 1024;
    }

    public int getId() { return id; }

    public boolean isLoaded() { return isLoaded; }

    public void load() { isLoaded = true; }

    public void unload() { isLoaded = false; }

    public int getAssignedMemory() { return assignedMemory; }

    public void setAddress(int address) { this.address = address; }

    public int getAddress() { return address; }
}

class MemoryManager {
    private Map<Integer, Page> pageTable;
    private int capacity;
    private double alpha;
    private double m;
    private double E;
    private List<VirtualMemory> virtualMemories;

    public MemoryManager(int capacity, double alpha, double m, double E) {
        this.capacity = capacity;
        this.pageTable = new HashMap<>();
        this.alpha = alpha;
        this.m = m;
        this.E = E;
        this.virtualMemories = new ArrayList<>();
    }

    public void requestPage(int pageId) {
        if (!pageTable.containsKey(pageId)) {
            System.out.println("Page " + pageId + " does not exist.");
            return;
        }

        Page page = pageTable.get(pageId);

        if (!page.isLoaded()) {
            if (pageTable.size() >= capacity) {
                replacePage(pageId);
            } else {
                loadPage(page);
            }
        }

        System.out.println("Page " + pageId + " accessed.");
    }

    private void loadPage(Page page) {
        page.load();
        System.out.println("Page " + page.getId() + " loaded into memory.");
    }

    private void replacePage(int newPageId) {
        Page victim = null;
        for (Page page : pageTable.values()) {
            if (!page.isLoaded()) {
                victim = page;
                break;
            }
        }
        if (victim != null) {
            victim.unload();
            loadPage(pageTable.get(newPageId));
            System.out.println("Page " + victim.getId() + " replaced by page " + newPageId);
        } else {
            System.out.println("Memory is full. Cannot replace any page.");
        }
    }

    public void addPage(Page page) {
        pageTable.put(page.getId(), page);
    }

    public void removePage(int pageId) {
        if (pageTable.containsKey(pageId)) {
            pageTable.remove(pageId);
            System.out.println("Page " + pageId + " removed from memory.");
        } else {
            System.out.println("Page " + pageId + " does not exist in memory.");
        }
    }

    public void displayPageTable() {
        System.out.println("Page Table:");
        for (Map.Entry<Integer, Page> entry : pageTable.entrySet()) {
            System.out.println("Page ID: " + entry.getKey() + ", Loaded: " + entry.getValue().isLoaded()
                    + ", Assigned Memory: " + entry.getValue().getAssignedMemory() / (1024 * 1024) + " MB, Address: "
                    + entry.getValue().getAddress() / (1024 * 1024) + " MB");
        }
    }

    public void assignAddresses() {
        int address = 0;
        for (Page page : pageTable.values()) {
            page.setAddress(address);
            address += page.getAssignedMemory();
        }
    }

    public boolean searchPageInTLB(int pageId) {
        return pageTable.containsKey(pageId);
    }

    public long calculateEAT(double alpha, double m, double E) {
        long mNano = (long) (m * 1e6);
        long ENano = (long) (E * 1e6);
        alpha /= 100.0;
        double alphaM = alpha * mNano;
        return (long) (2 * mNano + ENano - alphaM);
    }

    public void addVirtualMemory(VirtualMemory virtualMemory) {
        virtualMemories.add(virtualMemory);
    }
}

class VirtualMemory {
    private int index;
    private MemoryManager memoryManager;

    public VirtualMemory(int index, int physicalMemorySize) {
        this.index = index;
        this.memoryManager = new MemoryManager(physicalMemorySize, 0, 0, 0);
    }

    public int getIndex() {
        return index;
    }

    public MemoryManager getMemoryManager() {
        return memoryManager;
    }
}

public class VirtuaMM extends JFrame {
    private int numVirtualMemories = 0;

    public VirtuaMM() {
        setTitle("Virtual Memory Interface");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 200);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(4, 1));
        JLabel memorySizeLabel = new JLabel("Enter Total Size of Physical Memory (MB):");
        JTextField memorySizeField = new JTextField(10);
        panel.add(memorySizeLabel);
        panel.add(memorySizeField);

        JButton createButton = new JButton("Create Virtual Memory");
        createButton.addActionListener(e -> {
            int physicalMemorySize = Integer.parseInt(memorySizeField.getText());
            int virtualMemorySize = Integer.parseInt(JOptionPane.showInputDialog("Enter Size of Virtual Memory (MB):"));
            if (virtualMemorySize <= physicalMemorySize) {
                numVirtualMemories++;
                VirtualMemory vm = new VirtualMemory(numVirtualMemories, virtualMemorySize);
                openMemoryManager(vm);
                dispose();
            } else {
                JOptionPane.showMessageDialog(null, "Virtual memory size exceeds physical memory size.");
            }
        });

        panel.add(createButton);
        getContentPane().add(panel, BorderLayout.CENTER);
        setVisible(true);
    }

    private void openMemoryManager(VirtualMemory vm) {
        MemoryManager mm = vm.getMemoryManager();
        mm.addVirtualMemory(vm);

        JFrame frame = new JFrame("Memory Manager for VM " + vm.getIndex());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(400, 600);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(12, 1));
        JTextField pageIdField = new JTextField(10);
        JTextField pageMemoryField = new JTextField(10);

        panel.add(new JLabel("Enter Page ID:"));
        panel.add(pageIdField);
        panel.add(new JLabel("Enter Page Memory (MB):"));
        panel.add(pageMemoryField);

        JButton addPageBtn = new JButton("Add Page");
        addPageBtn.addActionListener(e -> {
            int pageId = Integer.parseInt(pageIdField.getText());
            int pageMemory = Integer.parseInt(pageMemoryField.getText());
            mm.addPage(new Page(pageId, pageMemory));
            JOptionPane.showMessageDialog(null, "Page added successfully!");
        });

        JButton displayBtn = new JButton("Display Page Table");
        displayBtn.addActionListener(e -> mm.displayPageTable());

        JButton tlbBtn = new JButton("Search Page in TLB");
        tlbBtn.addActionListener(e -> {
            int pageId = Integer.parseInt(pageIdField.getText());
            if (mm.searchPageInTLB(pageId)) {
                JOptionPane.showMessageDialog(null, "TLB Hit: Page " + pageId + " found.");
            } else {
                JOptionPane.showMessageDialog(null, "TLB Miss: Page " + pageId + " not found.");
            }
        });

        JButton eatBtn = new JButton("Calculate EAT");
        eatBtn.addActionListener(e -> {
            double alpha = Double.parseDouble(JOptionPane.showInputDialog("Enter alpha (%):"));
            double m = Double.parseDouble(JOptionPane.showInputDialog("Enter memory access time (m):"));
            double E = Double.parseDouble(JOptionPane.showInputDialog("Enter TLB lookup time (E):"));
            long eat = mm.calculateEAT(alpha, m, E);
            JOptionPane.showMessageDialog(null, "EAT: " + eat + " ns");
        });

        JButton optimalBtn = new JButton("Optimal Page Replacement");
        optimalBtn.addActionListener(e -> {
            JFrame optFrame = new JFrame("Optimal Page Replacement");
            optFrame.setSize(400, 300);
            optFrame.setLocationRelativeTo(frame);
            JPanel optPanel = new JPanel(new GridLayout(3, 2));

            JTextField refStrField = new JTextField(10);
            JTextField frameCountField = new JTextField(10);

            optPanel.add(new JLabel("Reference String (e.g., 70120304230321201701):"));
            optPanel.add(refStrField);
            optPanel.add(new JLabel("Number of Frames:"));
            optPanel.add(frameCountField);

            JButton submitBtn = new JButton("Submit");
            submitBtn.addActionListener(ev -> {
                String refStr = refStrField.getText();
                int frames = Integer.parseInt(frameCountField.getText());
                performOptimalPageReplacement(refStr, frames);
            });

            optPanel.add(submitBtn);
            optFrame.add(optPanel);
            optFrame.setVisible(true);
        });

        JButton syncTechniquesBtn = new JButton("Synchronization Techniques");
        syncTechniquesBtn.addActionListener(e -> showSynchronizationTechniques());

        JButton addVirtualBtn = new JButton("Add Virtual Memory");
        addVirtualBtn.addActionListener(e -> {
            new VirtualMemoryInterface();
            frame.dispose();
        });

        panel.add(addPageBtn);
        panel.add(displayBtn);
        panel.add(tlbBtn);
        panel.add(eatBtn);
        panel.add(optimalBtn);
        panel.add(syncTechniquesBtn);
        panel.add(addVirtualBtn);

        frame.add(panel);
        frame.setVisible(true);
    }

    private void showSynchronizationTechniques() {
        JFrame syncFrame = new JFrame("Synchronization Techniques");
        syncFrame.setSize(400, 400);
        syncFrame.setLocationRelativeTo(null);

        JPanel syncPanel = new JPanel(new GridLayout(6, 1));
        syncPanel.add(new JLabel("Choose a synchronization technique to simulate:"));

        JButton mutexBtn = new JButton("1. Mutex Lock");
        mutexBtn.addActionListener(ev -> JOptionPane.showMessageDialog(null, "Simulating Mutex Lock:\nOnly one thread can access the resource at a time."));

        JButton semaphoreBtn = new JButton("2. Semaphore");
        semaphoreBtn.addActionListener(ev -> JOptionPane.showMessageDialog(null, "Simulating Semaphore:\nA signaling mechanism for limited concurrent access."));

        JButton monitorBtn = new JButton("3. Monitor");
        monitorBtn.addActionListener(ev -> JOptionPane.showMessageDialog(null, "Simulating Monitor:\nEncapsulates mutual exclusion and condition synchronization."));

        JButton readersWritersBtn = new JButton("4. Readers-Writers Problem");
        readersWritersBtn.addActionListener(ev -> JOptionPane.showMessageDialog(null, "Simulating Readers-Writers:\nMultiple readers or one writer at a time."));

        JButton prodConsBtn = new JButton("5. Producer-Consumer Problem");
        prodConsBtn.addActionListener(ev -> JOptionPane.showMessageDialog(null, "Simulating Producer-Consumer:\nProducers produce items and consumers consume, synchronized via buffer."));

        syncPanel.add(mutexBtn);
        syncPanel.add(semaphoreBtn);
        syncPanel.add(monitorBtn);
        syncPanel.add(readersWritersBtn);
        syncPanel.add(prodConsBtn);

        syncFrame.add(syncPanel);
        syncFrame.setVisible(true);
    }

    private void performOptimalPageReplacement(String referenceString, int frames) {
        referenceString = referenceString.replaceAll("[^\\d]", "");
        int[] referenceArray = referenceString.chars().map(Character::getNumericValue).toArray();

        Set<Integer> memory = new HashSet<>();
        List<Integer> memoryList = new ArrayList<>();
        int pageFaults = 0;

        for (int i = 0; i < referenceArray.length; i++) {
            int currentPage = referenceArray[i];
            if (!memory.contains(currentPage)) {
                pageFaults++;
                if (memory.size() < frames) {
                    memory.add(currentPage);
                    memoryList.add(currentPage);
                } else {
                    int farthest = i + 1, idxToReplace = -1, maxDistance = -1;
                    for (int page : memoryList) {
                        int nextIndex = farthest;
                        for (int j = i + 1; j < referenceArray.length; j++) {
                            if (referenceArray[j] == page) {
                                nextIndex = j;
                                break;
                            }
                        }
                        if (nextIndex > maxDistance) {
                            maxDistance = nextIndex;
                            idxToReplace = page;
                        }
                    }
                    memory.remove(idxToReplace);
                    memoryList.remove((Integer) idxToReplace);
                    memory.add(currentPage);
                    memoryList.add(currentPage);
                }
            }
        }

        JOptionPane.showMessageDialog(null, "Page Faults using Optimal Replacement: " + pageFaults);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(VirtualMemoryInterface::new);
    }
}
