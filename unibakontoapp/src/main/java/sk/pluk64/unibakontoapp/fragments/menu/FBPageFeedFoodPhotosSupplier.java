package sk.pluk64.unibakontoapp.fragments.menu;

import android.os.Bundle;

import com.facebook.AccessToken;
import com.facebook.FacebookRequestError;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sk.pluk64.unibakonto.Util;
import sk.pluk64.unibakontoapp.FBUtils;
import sk.pluk64.unibakontoapp.Utils;
import sk.pluk64.unibakontoapp.meals.Menza;

public class FBPageFeedFoodPhotosSupplier implements FoodPhotosSupplier {
    private final Menza jedalen;

    public FBPageFeedFoodPhotosSupplier(Menza jedalen) {
        this.jedalen = jedalen;
    }

    @Override
    public List<FBPhoto> getPhotos() throws Utils.FBAuthenticationException, Util.ConnectionFailedException {
        GraphRequest getPostsRequest = createGetPostsRequest(jedalen.getFBid());

        GraphResponse graphResponse = getPostsRequest.executeAndWait();

        return processResponse(graphResponse);
    }

    private List<FBPhoto> processResponse(GraphResponse response) throws Utils.FBAuthenticationException, Util.ConnectionFailedException {
        ArrayList<FBPhoto> result = new ArrayList<>();

        int REQUESTS_LIMIT = 10;
        int requestCounter = 1;
        boolean lastPostIsTooOld = false;
        while (true) {
            FacebookRequestError error = response.getError();
            JSONObject responseJSONObject = response.getJSONObject();

            if (error != null) {
                int errorCode = error.getErrorCode();
                String errorType = error.getErrorType();
                if (errorCode == 102 || errorCode == 190 || "OAuthException".equals(errorType)) {
                    throw new Utils.FBAuthenticationException();
                } else {
                    throw new Util.ConnectionFailedException();
                }
            } else if (responseJSONObject != null) {
                JSONArray posts = responseJSONObject.optJSONArray("data");
                for (int i = 0; i < posts.length(); i++) {
                    JSONObject post = posts.optJSONObject(i);
                    if (post != null) {
                        Date createdTime = FBUtils.parseDate(post.optString("created_time"));

                        lastPostIsTooOld = !Utils.isAtMostXHoursOld(createdTime, 20);

                        if (!lastPostIsTooOld) {
                            if (hasOnlyOneAttachment(post)) {
                                parseSingleAttachment(result, createdTime, post);
                            } else {
                                if (!someAttachmentDescriptionEqualsPostMessage(post)) {
                                    String message = post.optString("message");
                                    if (!message.isEmpty()) {
                                        result.add(new FBPhoto()
                                            .setCaption(message)
                                            .setCreatedTime(createdTime)
                                        );
                                    }
                                }

                                JSONObject attachments = post.optJSONObject("attachments");
                                if (attachments != null) {
                                    parseAttachments(result, createdTime, attachments);
                                }
                            }
                        }
                    }
                }
            }

            if (lastPostIsTooOld || requestCounter == REQUESTS_LIMIT) {
                break;
            } else {
                GraphRequest nextPage = response.getRequestForPagedResults(GraphResponse.PagingDirection.NEXT);
                if (nextPage != null) {
                    requestCounter += 1;
                    response = nextPage.executeAndWait();
                }
            }
        }

        return result;
    }

    private void parseSingleAttachment(ArrayList<FBPhoto> result, Date createdTime, JSONObject post) {
        String message = post.optString("message");
        JSONObject attachments = post.optJSONObject("attachments");
        if (attachments != null) {
            JSONArray attachmentsArray = attachments.optJSONArray("data");
            if (attachmentsArray != null && attachmentsArray.length() == 1) {
                JSONObject attachment = attachmentsArray.optJSONObject(0);
                if (attachment != null) {
                    parseAttachment(result, createdTime, attachment, message);
                }
            }
        }
    }

    private void parseAttachments(ArrayList<FBPhoto> result, Date createdTime, JSONObject attachments) {
        JSONArray attachmentsArray = attachments.optJSONArray("data");
        if (attachmentsArray != null) {
            for (int i = 0; i < attachmentsArray.length(); i++) {
                JSONObject attachment = attachmentsArray.optJSONObject(i);
                if (attachment != null) {
                    parseAttachment(result, createdTime, attachment);
                }
            }
        }
    }

    private void parseAttachment(ArrayList<FBPhoto> result, Date createdTime, JSONObject attachment) {
        parseAttachment(result, createdTime, attachment, "");
    }

    private void parseAttachment(ArrayList<FBPhoto> result, Date createdTime, JSONObject attachment, String prefixMessage) {
        String attachmentType = attachment.optString("type");
        switch (attachmentType) {
            case "album":
                JSONObject subattachments = attachment.optJSONObject("subattachments");
                if (subattachments != null) {
                    parseAttachments(result, createdTime, subattachments);
                }
                break;
            case "photo":
                parsePhoto(result, attachment, createdTime, prefixMessage);
                break;
            default:
                String url = attachment.optString("url");
                String description = attachment.optString("description");
                if (!prefixMessage.isEmpty() && !prefixMessage.equals(description)) {
                    if (description.isEmpty()) {
                        description = prefixMessage;
                    } else {
                        description = prefixMessage + "\n\n" + description;
                    }
                }
                String caption;
                if (url.isEmpty()) {
                    caption = description;
                } else {
                    caption = description + "\n\n" + url;
                }
                result.add(new FBPhoto()
                    .setCreatedTime(createdTime)
                    .setCaption(caption)
                );
                break;
        }
    }

    private boolean hasOnlyOneAttachment(JSONObject post) {
        JSONObject attachments = post.optJSONObject("attachments");
        if (attachments != null) {
            JSONArray attachmentsArray = attachments.optJSONArray("data");
            if (attachmentsArray != null && attachmentsArray.length() == 1) {
                JSONObject attachment = attachmentsArray.optJSONObject(0);
                if (attachment != null) {
                    String type = attachment.optString("type");
                    if (!type.equals("album")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean someAttachmentDescriptionEqualsPostMessage(JSONObject post) {
        String message = post.optString("message");
        if (!message.isEmpty()) {
            JSONObject attachments = post.optJSONObject("attachments");
            if (attachments != null) {
                JSONArray attachmentsArray = attachments.optJSONArray("data");
                if (attachmentsArray != null) {
                    for (int i = 0; i < attachmentsArray.length(); i++) {
                        JSONObject attachment = attachmentsArray.optJSONObject(i);
                        if (attachment != null) {
                            String description = attachment.optString("description");
                            if (description.equals(message)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private void parsePhoto(ArrayList<FBPhoto> result, JSONObject subattachment, Date createdTime, String prefixMessage) {
        JSONObject media = subattachment.optJSONObject("media");
        if (media != null) {
            JSONObject image = media.optJSONObject("image");
            if (image != null) {
                int height = image.optInt("height");
                int width = image.optInt("width");
                String src = image.optString("src");
                if (height != 0 && width != 0 && !src.isEmpty()) {
                    String description = subattachment.optString("description");
                    if (!prefixMessage.isEmpty() && !prefixMessage.equals(description)) {
                        description = prefixMessage + "\n\n" +  description;
                    }
                    FBPhoto photo = new FBPhoto()
                        .setWidth(width)
                        .setHeight(height)
                        .setSource(src)
                        .setCaption(description)
                        .setCreatedTime(createdTime);
                    result.add(photo);
                }
            }
        }
    }

    private GraphRequest createGetPostsRequest(String menzaFBid) {
        GraphRequest request = new GraphRequest(
            AccessToken.getCurrentAccessToken(),
            "/" + menzaFBid + "/posts?limit=2",
            null,
            HttpMethod.GET
        );
        Bundle params = new Bundle();
        params.putString("fields", "created_time,message,attachments{description,media,type,url,subattachments.limit(100){description,media,type,url}}");
        request.setParameters(params);
        return request;
    }
}
