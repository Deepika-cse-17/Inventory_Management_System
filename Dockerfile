# Use an official OpenJDK image as the base
FROM openjdk:17

# Set the working directory inside the container
WORKDIR /app

# Copy all project files into the container
COPY . .

# Compile Java files
RUN javac Main.java

# Expose port 8080 (Render expects a port; console apps don’t need it strictly)
EXPOSE 8080

# Command to run your app
CMD ["java", "Main"]
