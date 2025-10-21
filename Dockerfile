# Use OpenJDK 17 as base
FROM openjdk:17

# Set working directory inside the container
WORKDIR /app

# Copy all files from your repo into the container
COPY . .

# Compile all Java files (or just InventoryWeb.java)
RUN javac InventoryWeb.java

# Expose port 8080 (Render expects a port)
EXPOSE 8080

# Command to run your app
CMD ["java", "InventoryWeb"]
