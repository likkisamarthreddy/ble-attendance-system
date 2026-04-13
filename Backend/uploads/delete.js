const admin = require("firebase-admin");

// Initialize Firebase Admin with your service account key
const serviceAccount = require("../serviceAccountKey.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

// Add the emails you want to exclude here
const EXCLUDED_EMAILS = ["admin1@gmail.com", "p1@gmail.com"];

async function deleteAllExcept(excludedEmails) {
  try {
    let nextPageToken;
    do {
      const listUsersResult = await admin.auth().listUsers(1000, nextPageToken);
      for (const user of listUsersResult.users) {
        if (!excludedEmails.includes(user.email)) {
          console.log(`Deleting user: ${user.email}`);
          await admin.auth().deleteUser(user.uid);
        } else {
          console.log(`Skipping user: ${user.email}`);
        }
      }
      nextPageToken = listUsersResult.pageToken;
    } while (nextPageToken);
    console.log("All non-excluded users have been deleted.");
  } catch (error) {
    console.error("Error deleting users:", error);
  }
}

deleteAllExcept(EXCLUDED_EMAILS);
