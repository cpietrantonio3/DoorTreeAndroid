package codewhale.doortreeandroid

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.CreditScore
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector

fun systemIcon(name: String): ImageVector {
    return when (name) {
        "house.fill" -> Icons.Rounded.Home
        "creditcard.fill", "creditcard.trianglebadge.exclamationmark" -> Icons.Rounded.CreditCard
        "wrench.and.screwdriver.fill", "wrench.and.screwdriver" -> Icons.Rounded.Handyman
        "bubble.left.fill" -> Icons.Rounded.ChatBubble
        "bubble.left.and.bubble.right.fill", "bubble.left.and.exclamationmark.bubble.right" -> Icons.Rounded.Forum
        "person.crop.circle.fill", "person.crop.circle.badge.exclamationmark" -> Icons.Rounded.AccountCircle
        "bell" -> Icons.Rounded.Notifications
        "bell.badge.fill" -> Icons.Rounded.NotificationsActive
        "bell.slash" -> Icons.Rounded.NotificationsOff
        "clock.arrow.circlepath" -> Icons.Rounded.History
        "square.and.arrow.up" -> Icons.Rounded.IosShare
        "arrow.up.right.square" -> Icons.Rounded.OpenInNew
        "paperplane.fill" -> Icons.Rounded.Send
        "envelope.fill", "envelope.badge.fill" -> Icons.Rounded.Email
        "lock.fill" -> Icons.Rounded.Lock
        "lock.shield" -> Icons.Rounded.Shield
        "eye" -> Icons.Rounded.Visibility
        "eye.slash" -> Icons.Rounded.VisibilityOff
        "chevron.left" -> Icons.Rounded.ArrowBack
        "chevron.right" -> Icons.Rounded.ArrowForward
        "xmark" -> Icons.Rounded.Close
        "person.crop.rectangle.stack.fill" -> Icons.Rounded.Person
        "key.fill" -> Icons.Rounded.Key
        "calendar", "calendar.badge.clock" -> Icons.Rounded.CalendarMonth
        "doc.text", "doc.fill", "doc.text.fill", "doc.badge.clock" -> Icons.Rounded.Description
        "arrow.down.circle.fill" -> Icons.Rounded.Download
        "doc.viewfinder" -> Icons.Rounded.UploadFile
        "dollarsign.circle.fill", "building.columns.fill" -> Icons.Rounded.AccountBalance
        "camera.fill" -> Icons.Rounded.PhotoCamera
        "photo", "photo.fill", "photo.on.rectangle", "photo.on.rectangle.angled" -> Icons.Rounded.Photo
        "drop.fill" -> Icons.Rounded.Opacity
        "bolt.fill" -> Icons.Rounded.Bolt
        "fan.fill" -> Icons.Rounded.Schedule
        "flame.fill" -> Icons.Rounded.Whatshot
        "snowflake", "snowflake.circle.fill" -> Icons.Rounded.AcUnit
        "washer.fill" -> Icons.Rounded.LocalLaundryService
        "hammer.fill", "square.split.diagonal.2x2.fill" -> Icons.Rounded.Construction
        "paintbrush.fill" -> Icons.Rounded.Brush
        "square.grid.3x3.fill" -> Icons.Rounded.Dashboard
        "ladybug.fill" -> Icons.Rounded.BugReport
        "sparkles" -> Icons.Rounded.AutoAwesome
        "building.2.fill" -> Icons.Rounded.Apartment
        "leaf.fill" -> Icons.Rounded.Park
        "rectangle.split.3x1.fill" -> Icons.Rounded.ViewWeek
        "drop.triangle.fill" -> Icons.Rounded.WaterDrop
        "aqi.medium" -> Icons.Rounded.Dehaze
        "checklist" -> Icons.Rounded.FactCheck
        "exclamationmark.triangle.fill" -> Icons.Rounded.WarningAmber
        "ellipsis.circle.fill" -> Icons.Rounded.MoreHoriz
        "paperclip" -> Icons.Rounded.AttachFile
        "arrow.up" -> Icons.Rounded.ArrowUpward
        "faceid" -> Icons.Rounded.Face
        "creditscore" -> Icons.Rounded.CreditScore
        "pencil" -> Icons.Rounded.Edit
        else -> Icons.Rounded.ErrorOutline
    }
}
