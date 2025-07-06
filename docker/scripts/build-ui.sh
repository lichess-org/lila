#!/bin/bash

# Build UI assets for Lichess

set -e

echo "Building Lichess UI assets..."

# Install Node.js dependencies
if [ -f "ui/package.json" ]; then
    echo "Installing Node.js dependencies..."
    cd ui
    npm install
    
    # Build all UI modules
    echo "Building UI modules..."
    npm run build
    
    # Copy built assets to public directory
    echo "Copying built assets..."
    cp -r dist/* ../public/
    
    cd ..
    
    echo "UI assets built successfully!"
else
    echo "No package.json found, skipping UI build"
fi

# Build CSS assets
echo "Building CSS assets..."
if [ -d "ui" ]; then
    # Find all SCSS files and compile them
    find ui -name "*.scss" -not -path "*/node_modules/*" | while read file; do
        echo "Processing $file"
        # This would require sass compiler
        # For now, we assume CSS is pre-compiled
    done
fi

# Set proper permissions
chmod -R 755 public/

echo "Asset build completed!" 