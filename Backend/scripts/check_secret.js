const crypto = require('crypto');
require('dotenv').config();

function deriveCourseSecret(courseId) {
  const masterSecret = process.env.BLE_SESSION_SECRET;
  return crypto
    .createHmac("sha256", masterSecret)
    .update(courseId.toString())
    .digest("hex");
}

const courseId = "69afae24422bfdc2a33683e9";
console.log("Derived Secret for " + courseId + ": " + deriveCourseSecret(courseId));
