package com.mindquest.model;

import com.mindquest.model.question.Question;
import com.mindquest.model.question.EasyQuestion;
import com.mindquest.model.question.MediumQuestion;
import com.mindquest.model.question.HardQuestion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestionBank {
    private Map<String, List<Question>> questionsByTopicAndDifficulty;
    private int csEasyCounter = 0;
    private int csMediumCounter = 0;
    private int csHardCounter = 0;
    private int aiEasyCounter = 0;
    private int aiMediumCounter = 0;
    private int aiHardCounter = 0;
    private int philosophyEasyCounter = 0;
    private int philosophyMediumCounter = 0;
    private int philosophyHardCounter = 0;

    public QuestionBank() {
        questionsByTopicAndDifficulty = new HashMap<>();
        questionsByTopicAndDifficulty.put("Computer Science_Easy", new ArrayList<>());
        questionsByTopicAndDifficulty.put("Computer Science_Medium", new ArrayList<>());
        questionsByTopicAndDifficulty.put("Computer Science_Hard", new ArrayList<>());
        questionsByTopicAndDifficulty.put("Artificial Intelligence_Easy", new ArrayList<>());
        questionsByTopicAndDifficulty.put("Artificial Intelligence_Medium", new ArrayList<>());
        questionsByTopicAndDifficulty.put("Artificial Intelligence_Hard", new ArrayList<>());
        questionsByTopicAndDifficulty.put("Philosophy_Easy", new ArrayList<>());
        questionsByTopicAndDifficulty.put("Philosophy_Medium", new ArrayList<>());
        questionsByTopicAndDifficulty.put("Philosophy_Hard", new ArrayList<>());
        loadDefaultQuestions();
    }

    private String generateId(String topic, String difficulty) {
        String prefix = topic.substring(0, Math.min(topic.length(), 2)).toUpperCase() + "_" + difficulty.toUpperCase().substring(0, Math.min(difficulty.length(), 4)).toUpperCase();
        int counter;
        switch (topic + "_" + difficulty) {
            case "Computer Science_Easy":
                counter = ++csEasyCounter;
                break;
            case "Computer Science_Medium":
                counter = ++csMediumCounter;
                break;
            case "Computer Science_Hard":
                counter = ++csHardCounter;
                break;
            case "Artificial Intelligence_Easy":
                counter = ++aiEasyCounter;
                break;
            case "Artificial Intelligence_Medium":
                counter = ++aiMediumCounter;
                break;
            case "Artificial Intelligence_Hard":
                counter = ++aiHardCounter;
                break;
            case "Philosophy_Easy":
                counter = ++philosophyEasyCounter;
                break;
            case "Philosophy_Medium":
                counter = ++philosophyMediumCounter;
                break;
            case "Philosophy_Hard":
                counter = ++philosophyHardCounter;
                break;
            default:
                counter = 0; // Should not happen
        }
        return String.format("%s_%03d", prefix, counter);
    }

    public void loadDefaultQuestions() {
        // Computer Science - Easy
        List<String> csEasyChoices1 = List.of("Store long-term data", "Execute program instructions", "Display graphics", "Connect to the internet");
        questionsByTopicAndDifficulty.get("Computer Science_Easy").add(new EasyQuestion(generateId("Computer Science", "Easy"), "What is the primary function of a Central Processing Unit (CPU)?", csEasyChoices1, 1, "Computer Science"));

        List<String> csEasyChoices2 = List.of("Monitor", "Printer", "Keyboard", "Speakers");
        questionsByTopicAndDifficulty.get("Computer Science_Easy").add(new EasyQuestion(generateId("Computer Science", "Easy"), "Which of the following is a common input device for a computer?", csEasyChoices2, 2, "Computer Science"));

        List<String> csEasyChoices3 = List.of("Read Access Memory", "Random Access Memory", "Remote Access Module", "Run Application Management");
        questionsByTopicAndDifficulty.get("Computer Science_Easy").add(new EasyQuestion(generateId("Computer Science", "Easy"), "What does RAM stand for in computing?", csEasyChoices3, 1, "Computer Science"));

        List<String> csEasyChoices4 = List.of("CPU", "RAM", "Hard Drive", "Graphics Card");
        questionsByTopicAndDifficulty.get("Computer Science_Easy").add(new EasyQuestion(generateId("Computer Science", "Easy"), "Which component is responsible for storing data permanently in a computer?", csEasyChoices4, 2, "Computer Science"));

        List<String> csEasyChoices5 = List.of("A type of network cable", "A set of rules for data communication", "A physical network device", "Software for browsing the internet");
        questionsByTopicAndDifficulty.get("Computer Science_Easy").add(new EasyQuestion(generateId("Computer Science", "Easy"), "What is a network protocol?", csEasyChoices5, 1, "Computer Science"));

        // Computer Science - Medium
        List<String> csMediumChoices1 = List.of("Queue", "Stack", "Linked List", "Tree");
        questionsByTopicAndDifficulty.get("Computer Science_Medium").add(new MediumQuestion(generateId("Computer Science", "Medium"), "Which data structure uses LIFO (Last In, First Out) principle?", csMediumChoices1, 1, "Computer Science"));

        List<String> csMediumChoices2 = List.of("O(n)", "O(log n)", "O(n log n)", "O(1)");
        questionsByTopicAndDifficulty.get("Computer Science_Medium").add(new MediumQuestion(generateId("Computer Science", "Medium"), "What is the time complexity of searching an element in a sorted array using binary search?", csMediumChoices2, 1, "Computer Science"));

        List<String> csMediumChoices3 = List.of("The ability of an object to take on many forms", "Hiding the implementation details of an object", "Bundling data and methods that operate on the data", "Creating new classes from existing classes");
        questionsByTopicAndDifficulty.get("Computer Science_Medium").add(new MediumQuestion(generateId("Computer Science", "Medium"), "In object-oriented programming, what is polymorphism?", csMediumChoices3, 0, "Computer Science"));

        List<String> csMediumChoices4 = List.of("Storing elements in a sorted order", "Implementing a FIFO queue", "Fast lookups by key", "Representing hierarchical data");
        questionsByTopicAndDifficulty.get("Computer Science_Medium").add(new MediumQuestion(generateId("Computer Science", "Medium"), "Which of the following is a common use case for a hash map (or dictionary)?", csMediumChoices4, 2, "Computer Science"));

        List<String> csMediumChoices5 = List.of("To execute code only if an exception occurs", "To execute code only if no exception occurs", "To execute code regardless of whether an exception occurred", "To define a new exception type");
        questionsByTopicAndDifficulty.get("Computer Science_Medium").add(new MediumQuestion(generateId("Computer Science", "Medium"), "What is the primary purpose of a 'finally' block in a try-catch-finally statement in Java?", csMediumChoices5, 2, "Computer Science"));

        // Computer Science - Hard
        List<String> csHardChoices1 = List.of("Consistency, Availability, Partition tolerance; you can achieve all three simultaneously", "Consistency, Atomicity, Partition tolerance; you can achieve any two", "Consistency, Availability, Partition tolerance; you can only achieve two out of three", "Concurrency, Availability, Performance; you can achieve all three");
        questionsByTopicAndDifficulty.get("Computer Science_Hard").add(new HardQuestion(generateId("Computer Science", "Hard"), "What is the CAP theorem in distributed systems?", csHardChoices1, 2, "Computer Science"));

        List<String> csHardChoices2 = List.of("A situation where a process is unable to acquire a resource", "A situation where two or more processes are blocked indefinitely, waiting for each other to release resources", "A process that has terminated unexpectedly", "A condition where a process repeatedly requests a resource that is immediately granted");
        questionsByTopicAndDifficulty.get("Computer Science_Hard").add(new HardQuestion(generateId("Computer Science", "Hard"), "Which of the following best describes a \"deadlock\" in an operating system?", csHardChoices2, 1, "Computer Science"));

        List<String> csHardChoices3 = List.of("Atomicity, Consistency, Isolation, Durability", "Availability, Consistency, Integrity, Durability", "Atomicity, Concurrency, Isolation, Distribution", "Access, Control, Integrity, Data");
        questionsByTopicAndDifficulty.get("Computer Science_Hard").add(new HardQuestion(generateId("Computer Science", "Hard"), "In the context of databases, what is \"ACID\" a mnemonic for?", csHardChoices3, 0, "Computer Science"));

        List<String> csHardChoices4 = List.of("Simpler deployment and testing", "Reduced operational overhead", "Increased coupling between components", "Independent deployability and scalability of services");
        questionsByTopicAndDifficulty.get("Computer Science_Hard").add(new HardQuestion(generateId("Computer Science", "Hard"), "What is the primary advantage of using a microservices architecture over a monolithic architecture?", csHardChoices4, 3, "Computer Science"));

        List<String> csHardChoices5 = List.of("Bubble Sort", "Insertion Sort", "Quick Sort", "Merge Sort");
        questionsByTopicAndDifficulty.get("Computer Science_Hard").add(new HardQuestion(generateId("Computer Science", "Hard"), "Which sorting algorithm has the worst-case time complexity of O(n^2) but a best-case and average-case time complexity of O(n log n)?", csHardChoices5, 2, "Computer Science"));
        // Artificial Intelligence - Easy
        List<String> aiEasyChoices1 = List.of("Natural Language Processing", "Robotics", "Computer Vision", "Expert Systems");
        questionsByTopicAndDifficulty.get("Artificial Intelligence_Easy").add(new EasyQuestion(generateId("Artificial Intelligence", "Easy"), "Which field of AI focuses on enabling computers to \"see\" and interpret visual information?", aiEasyChoices1, 2, "Artificial Intelligence"));

        List<String> aiEasyChoices2 = List.of("Neural Network", "Chatbot", "Algorithm", "Data Miner");
        questionsByTopicAndDifficulty.get("Artificial Intelligence_Easy").add(new EasyQuestion(generateId("Artificial Intelligence", "Easy"), "What is a common term for an AI program designed to simulate human conversation?", aiEasyChoices2, 1, "Artificial Intelligence"));

        List<String> aiEasyChoices3 = List.of("Unsupervised Learning", "Reinforcement Learning", "Supervised Learning", "Deep Learning");
        questionsByTopicAndDifficulty.get("Artificial Intelligence_Easy").add(new EasyQuestion(generateId("Artificial Intelligence", "Easy"), "Which type of learning in AI involves training a model on labeled data?", aiEasyChoices3, 2, "Artificial Intelligence"));

        List<String> aiEasyChoices4 = List.of("To understand human emotions", "To mimic human creativity", "To achieve optimal performance in a defined environment", "To generate random moves");
        questionsByTopicAndDifficulty.get("Artificial Intelligence_Easy").add(new EasyQuestion(generateId("Artificial Intelligence", "Easy"), "What is the goal of an AI system that plays games like chess or Go?", aiEasyChoices4, 2, "Artificial Intelligence"));

        List<String> aiEasyChoices5 = List.of("Automated Intelligence", "Artificial Information", "Advanced Integration", "Artificial Intelligence");
        questionsByTopicAndDifficulty.get("Artificial Intelligence_Easy").add(new EasyQuestion(generateId("Artificial Intelligence", "Easy"), "What does \"AI\" stand for?", aiEasyChoices5, 3, "Artificial Intelligence"));

        // Artificial Intelligence - Medium
        List<String> aiMediumChoices1 = List.of("Supervised Learning", "Unsupervised Learning", "Reinforcement Learning", "Transfer Learning");
        questionsByTopicAndDifficulty.get("Artificial Intelligence_Medium").add(new MediumQuestion(generateId("Artificial Intelligence", "Medium"), "Which AI concept involves a machine learning from its own actions and experiences through trial and error, often in a simulated environment?", aiMediumChoices1, 2, "Artificial Intelligence"));

        List<String> aiMediumChoices2 = List.of("To test the model's performance", "To provide data for the model to learn patterns and relationships", "To validate the model's accuracy", "To fine-tune hyperparameters");
        questionsByTopicAndDifficulty.get("Artificial Intelligence_Medium").add(new MediumQuestion(generateId("Artificial Intelligence", "Medium"), "What is the primary purpose of a \"training set\" in machine learning?", aiMediumChoices2, 1, "Artificial Intelligence"));

        List<String> aiMediumChoices3 = List.of("K-Means Clustering", "Linear Regression", "Support Vector Machine (SVM)", "Decision Tree");
        questionsByTopicAndDifficulty.get("Artificial Intelligence_Medium").add(new MediumQuestion(generateId("Artificial Intelligence", "Medium"), "Which algorithm is commonly used for classification tasks and works by finding a hyperplane that best separates different classes in a dataset?", aiMediumChoices3, 2, "Artificial Intelligence"));

        List<String> aiMediumChoices4 = List.of("The study of natural ecosystems", "A branch of AI that enables computers to understand, interpret, and generate human language", "A method for processing natural images", "The development of natural user interfaces");
        questionsByTopicAndDifficulty.get("Artificial Intelligence_Medium").add(new MediumQuestion(generateId("Artificial Intelligence", "Medium"), "What is \"Natural Language Processing\" (NLP)?", aiMediumChoices4, 1, "Artificial Intelligence"));

        List<String> aiMediumChoices5 = List.of("The input layer of the network", "The output layer of the network", "A layer of neurons between the input and output layers that performs computations", "A layer that is not visible to the programmer");
        questionsByTopicAndDifficulty.get("Artificial Intelligence_Medium").add(new MediumQuestion(generateId("Artificial Intelligence", "Medium"), "In the context of neural networks, what is a \"hidden layer\"?", aiMediumChoices5, 2, "Artificial Intelligence"));

        // Artificial Intelligence - Hard
        List<String> aiHardChoices1 = List.of("Gradients become too large, leading to unstable training", "Gradients become too small, making it difficult for the network to learn from earlier layers", "The network's accuracy decreases rapidly", "The network overfits the training data");
        questionsByTopicAndDifficulty.get("Artificial Intelligence_Hard").add(new HardQuestion(generateId("Artificial Intelligence", "Hard"), "What is the \"vanishing gradient problem\" in training deep neural networks?", aiHardChoices1, 1, "Artificial Intelligence"));

        List<String> aiHardChoices2 = List.of("Symbolic AI", "Connectionism", "Agent-Based AI", "Evolutionary Computation");
        questionsByTopicAndDifficulty.get("Artificial Intelligence_Hard").add(new HardQuestion(generateId("Artificial Intelligence", "Hard"), "Which AI paradigm focuses on creating intelligent agents that perceive their environment and take actions to maximize their chances of achieving their goals?", aiHardChoices2, 2, "Artificial Intelligence"));

        List<String> aiHardChoices3 = List.of("Improving the accuracy of classification models", "Generating realistic new data instances that resemble the training data", "Reducing the computational cost of deep learning", "Preventing overfitting in neural networks");
        questionsByTopicAndDifficulty.get("Artificial Intelligence_Hard").add(new HardQuestion(generateId("Artificial Intelligence", "Hard"), "What is the primary challenge addressed by \"Generative Adversarial Networks\" (GANs)?", aiHardChoices3, 1, "Artificial Intelligence"));

        List<String> aiHardChoices4 = List.of("Training a model from scratch on a new dataset", "Applying knowledge gained from solving one problem to a different but related problem", "Transferring data between different machine learning models", "Learning multiple tasks simultaneously with a single model");
        questionsByTopicAndDifficulty.get("Artificial Intelligence_Hard").add(new HardQuestion(generateId("Artificial Intelligence", "Hard"), "Explain the concept of \"Transfer Learning\" in machine learning.", aiHardChoices4, 1, "Artificial Intelligence"));

        List<String> aiHardChoices5 = List.of("A computer's ability to solve complex mathematical problems", "A machine's ability to exhibit intelligent behavior equivalent to, or indistinguishable from, that of a human", "The speed of a computer's processing unit", "The efficiency of an AI algorithm");
        questionsByTopicAndDifficulty.get("Artificial Intelligence_Hard").add(new HardQuestion(generateId("Artificial Intelligence", "Hard"), "What is the \"Turing Test\" designed to assess?", aiHardChoices5, 1, "Artificial Intelligence"));
        // Philosophy - Easy
        List<String> philEasyChoices1 = List.of("Plato", "Aristotle", "Socrates", "Pythagoras");
        questionsByTopicAndDifficulty.get("Philosophy_Easy").add(new EasyQuestion(generateId("Philosophy", "Easy"), "Which ancient Greek philosopher is famous for his method of questioning to stimulate critical thinking, often summarized as \"the Socratic method\"?", philEasyChoices1, 2, "Philosophy"));

        List<String> philEasyChoices2 = List.of("Metaphysics", "Ethics", "Epistemology", "Aesthetics");
        questionsByTopicAndDifficulty.get("Philosophy_Easy").add(new EasyQuestion(generateId("Philosophy", "Easy"), "What branch of philosophy deals with the nature of knowledge, justification, and belief?", philEasyChoices2, 2, "Philosophy"));

        List<String> philEasyChoices3 = List.of("Stoicism", "Hedonism", "Nihilism", "Existentialism");
        questionsByTopicAndDifficulty.get("Philosophy_Easy").add(new EasyQuestion(generateId("Philosophy", "Easy"), "Which philosophical concept suggests that pleasure is the highest good and proper aim of human life?", philEasyChoices3, 1, "Philosophy"));

        List<String> philEasyChoices4 = List.of("Always seek personal gain", "Treat others as you would like to be treated", "The strongest survive", "Follow the laws of the land");
        questionsByTopicAndDifficulty.get("Philosophy_Easy").add(new EasyQuestion(generateId("Philosophy", "Easy"), "What is the \"Golden Rule\" in ethics?", philEasyChoices4, 1, "Philosophy"));

        List<String> philEasyChoices5 = List.of("John Locke", "David Hume", "René Descartes", "Immanuel Kant");
        questionsByTopicAndDifficulty.get("Philosophy_Easy").add(new EasyQuestion(generateId("Philosophy", "Easy"), "Which philosopher is known for the famous phrase, \"I think, therefore I am\"?", philEasyChoices5, 2, "Philosophy"));

        // Philosophy - Medium
        List<String> philMediumChoices1 = List.of("Epicureanism", "Stoicism", "Rationalism", "Empiricism");
        questionsByTopicAndDifficulty.get("Philosophy_Medium").add(new MediumQuestion(generateId("Philosophy", "Medium"), "Which philosophical school of thought emphasizes living in harmony with nature and reason, often advocating for emotional resilience and self-control?", philMediumChoices1, 1, "Philosophy"));

        List<String> philMediumChoices2 = List.of("A story about a hidden treasure, by Aristotle", "A metaphor for the effect of education on the human soul, by Plato", "A tale of a journey through a dark forest, by Socrates", "A description of early human civilization, by Rousseau");
        questionsByTopicAndDifficulty.get("Philosophy_Medium").add(new MediumQuestion(generateId("Philosophy", "Medium"), "What is the \"Allegory of the Cave\" and which philosopher proposed it?", philMediumChoices2, 1, "Philosophy"));

        List<String> philMediumChoices3 = List.of("Deontology", "Virtue Ethics", "Consequentialism", "Existentialism");
        questionsByTopicAndDifficulty.get("Philosophy_Medium").add(new MediumQuestion(generateId("Philosophy", "Medium"), "Which ethical framework judges the morality of an action based on its outcome or consequences?", philMediumChoices3, 2, "Philosophy"));

        List<String> philMediumChoices4 = List.of("A command to act only out of self-interest, by Machiavelli", "A moral law that is unconditional or absolute for all agents, by Immanuel Kant", "A rule for achieving happiness, by Epicurus", "A principle of utility, by Jeremy Bentham");
        questionsByTopicAndDifficulty.get("Philosophy_Medium").add(new MediumQuestion(generateId("Philosophy", "Medium"), "What is the \"Categorical Imperative\" and which philosopher is associated with it?", philMediumChoices4, 1, "Philosophy"));

        List<String> philMediumChoices5 = List.of("The Problem of Evil", "The Mind-Body Problem", "The Problem of Induction", "The Problem of External World Skepticism");
        questionsByTopicAndDifficulty.get("Philosophy_Medium").add(new MediumQuestion(generateId("Philosophy", "Medium"), "Which philosophical problem explores whether we can truly know the external world, or if our knowledge is limited to our perceptions and ideas?", philMediumChoices5, 3, "Philosophy"));

        // Philosophy - Hard
        List<String> philHardChoices1 = List.of("The idea that the mind is born with innate knowledge", "The theory that the mind is a \"blank slate\" at birth, with all knowledge derived from experience", "A form of logical fallacy", "A state of mental confusion");
        questionsByTopicAndDifficulty.get("Philosophy_Hard").add(new HardQuestion(generateId("Philosophy", "Hard"), "Explain the concept of \"Tabula Rasa\" and its significance in the philosophy of mind.", philHardChoices1, 1, "Philosophy"));

        List<String> philHardChoices2 = List.of("Arthur Schopenhauer", "Søren Kierkegaard", "Friedrich Nietzsche", "Jean-Paul Sartre");
        questionsByTopicAndDifficulty.get("Philosophy_Hard").add(new HardQuestion(generateId("Philosophy", "Hard"), "Which philosopher is known for his critique of metaphysics and his emphasis on the \"will to power\" as a fundamental driving force in human existence?", philHardChoices2, 2, "Philosophy"));

        List<String> philHardChoices3 = List.of("A paradox about time travel, exploring causality", "A thought experiment about identity, asking if an object remains the same after all its components are replaced", "A logical puzzle about infinite regress, exploring epistemology", "A moral dilemma about sacrifice, exploring ethics");
        questionsByTopicAndDifficulty.get("Philosophy_Hard").add(new HardQuestion(generateId("Philosophy", "Hard"), "What is the \"Ship of Theseus\" paradox, and what philosophical concept does it explore?", philHardChoices3, 1, "Philosophy"));

        List<String> philHardChoices4 = List.of("Our predetermined nature defines who we are", "We are born with a fixed purpose", "We first exist, then define ourselves through our choices and actions", "Essence is more important than existence");
        questionsByTopicAndDifficulty.get("Philosophy_Hard").add(new HardQuestion(generateId("Philosophy", "Hard"), "In existentialism, what does \"existence precedes essence\" mean?", philHardChoices4, 2, "Philosophy"));

        List<String> philHardChoices5 = List.of("The Ontological Argument", "The Teleological Argument", "The Cosmological Argument", "The Moral Argument");
        questionsByTopicAndDifficulty.get("Philosophy_Hard").add(new HardQuestion(generateId("Philosophy", "Hard"), "Which philosophical argument attempts to prove the existence of God by appealing to the necessity of a first cause or an uncaused cause?", philHardChoices5, 2, "Philosophy"));
    }

    public List<Question> getQuestionsByTopicAndDifficulty(String topic, String difficulty) {
        return questionsByTopicAndDifficulty.get(topic + "_" + difficulty);
    }
}