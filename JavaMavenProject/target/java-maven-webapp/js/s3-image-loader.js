// Simple S3 Image Loader for Maven Web Application
// Uses servlet to fetch objects from S3 with Instance Profile credentials

/**
 * Fetch object from S3 through servlet
 * @param {string} key - S3 object key (filename)
 * @returns {Promise<Response>} - Fetch response
 */
async function fetchObjectWithRole(key) {
    try {
        const response = await fetch(`./object/${key}`);
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        return response;
    } catch (error) {
        console.error(`Error fetching ${key}:`, error);
        throw error;
    }
}

/**
 * Load and display a single image
 * @param {string} imageKey - S3 image key
 */
async function loadImage(imageKey) {
    const container = document.getElementById('image-container');
    container.innerHTML = '<p>🔄 Loading image...</p>';
    
    try {
        const response = await fetchObjectWithRole(imageKey);
        const blob = await response.blob();
        const imageUrl = URL.createObjectURL(blob);
        
        container.innerHTML = `
            <h3>📸 ${imageKey}</h3>
            <img src="${imageUrl}" alt="${imageKey}" style="max-width: 500px; border: 2px solid #ddd; border-radius: 8px;">
            <p>✅ Successfully loaded from S3!</p>
        `;
    } catch (error) {
        container.innerHTML = `
            <h3>❌ Error loading ${imageKey}</h3>
            <p>Error: ${error.message}</p>
            <p>Make sure the file exists in S3 bucket: <strong>public-miyachinenn</strong></p>
        `;
    }
}

/**
 * Load multiple images
 * @param {string[]} imageKeys - Array of S3 image keys
 */
async function loadMultipleImages(imageKeys) {
    const container = document.getElementById('image-container');
    container.innerHTML = '<p>🔄 Loading multiple images...</p>';
    
    let html = '<div style="display: flex; flex-wrap: wrap; gap: 20px; justify-content: center;">';
    
    for (const imageKey of imageKeys) {
        try {
            const response = await fetchObjectWithRole(imageKey);
            const blob = await response.blob();
            const imageUrl = URL.createObjectURL(blob);
            
            html += `
                <div style="text-align: center; margin: 10px;">
                    <h4>${imageKey}</h4>
                    <img src="${imageUrl}" alt="${imageKey}" style="max-width: 250px; border: 1px solid #ddd; border-radius: 5px;">
                    <p>✅ Loaded</p>
                </div>
            `;
        } catch (error) {
            html += `
                <div style="text-align: center; margin: 10px; color: red;">
                    <h4>${imageKey}</h4>
                    <p>❌ Failed to load</p>
                    <p>${error.message}</p>
                </div>
            `;
        }
    }
    
    html += '</div>';
    container.innerHTML = html;
}

/**
 * Test servlet connectivity
 */
async function testServlet() {
    const container = document.getElementById('image-container');
    container.innerHTML = '<p>🔍 Testing servlet connection...</p>';
    
    try {
        // Test with a known image
        const response = await fetchObjectWithRole('image.jpg');
        const contentType = response.headers.get('content-type');
        const contentLength = response.headers.get('content-length');
        
        container.innerHTML = `
            <h3>✅ Servlet Test Successful!</h3>
            <div style="background: #e8f5e8; padding: 15px; border-radius: 5px; text-align: left;">
                <p><strong>Response Details:</strong></p>
                <ul>
                    <li>Status: ${response.status} ${response.statusText}</li>
                    <li>Content-Type: ${contentType || 'Not specified'}</li>
                    <li>Content-Length: ${contentLength || 'Not specified'} bytes</li>
                    <li>Servlet URL: <code>./object/image.jpg</code></li>
                </ul>
                <p>🎉 Your servlet is working correctly with Instance Profile credentials!</p>
            </div>
        `;
    } catch (error) {
        container.innerHTML = `
            <h3>❌ Servlet Test Failed</h3>
            <div style="background: #ffe8e8; padding: 15px; border-radius: 5px; text-align: left;">
                <p><strong>Error Details:</strong></p>
                <ul>
                    <li>Error: ${error.message}</li>
                    <li>Check that Tomcat is running</li>
                    <li>Verify EC2 instance has IAM role attached</li>
                    <li>Ensure image.jpg exists in S3 bucket</li>
                </ul>
            </div>
        `;
    }
}

/**
 * Load all available images
 */
async function loadAllImages() {
    const allImages = ['cyrene.png', 'image.jpg', 'yuki.png'];
    await loadMultipleImages(allImages);
}

/**
 * View full image in a modal-like container
 * @param {string} imageKey - S3 image key
 */
async function viewFullImage(imageKey) {
    const container = document.getElementById('image-container');
    container.innerHTML = '<p>🔄 Loading full-size image...</p>';
    
    try {
        const response = await fetchObjectWithRole(imageKey);
        const blob = await response.blob();
        const imageUrl = URL.createObjectURL(blob);
        
        container.innerHTML = `
            <div style="text-align: center; padding: 20px;">
                <h3 style="color: #2c3e50; margin-bottom: 20px;">📸 ${imageKey}</h3>
                <div style="background: white; display: inline-block; padding: 10px; border-radius: 10px; box-shadow: 0 4px 15px rgba(0,0,0,0.1);">
                    <img src="${imageUrl}" alt="${imageKey}" style="max-width: 100%; max-height: 500px; border-radius: 8px; display: block;">
                </div>
                <div style="margin-top: 15px;">
                    <button class="button" onclick="downloadImage('${imageKey}')" style="margin: 5px;">💾 Download</button>
                    <button class="button" onclick="loadAllImages()" style="margin: 5px;">🖼️ Show All</button>
                </div>
                <p style="color: #666; margin-top: 10px;">✅ Successfully loaded from S3 bucket: <strong>public-miyachinenn</strong></p>
            </div>
        `;
    } catch (error) {
        container.innerHTML = `
            <div style="text-align: center; padding: 20px;">
                <h3 style="color: #e74c3c;">❌ Error loading ${imageKey}</h3>
                <p>Error: ${error.message}</p>
                <p>Make sure the file exists in S3 bucket: <strong>public-miyachinenn</strong></p>
            </div>
        `;
    }
}

/**
 * Download image function
 * @param {string} imageKey - S3 image key
 */
function downloadImage(imageKey) {
    const link = document.createElement('a');
    link.href = `./object/${imageKey}`;
    link.download = imageKey;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

// Auto-load functionality when page loads
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 S3 Image Loader initialized');
    console.log('Available functions: loadImage(), loadMultipleImages(), testServlet(), fetchObjectWithRole(), viewFullImage(), loadAllImages()');
    console.log('Available images: cyrene.png, image.jpg, yuki.png');
});